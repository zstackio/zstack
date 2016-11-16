package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.image.*;
import org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotEvent.Failure;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.search.SearchOp;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.CreateTemplateFromVmRootVolumeMsg;
import org.zstack.header.vm.CreateTemplateFromVmRootVolumeReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.RunOnce;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

public class ImageManagerImpl extends AbstractService implements ImageManager, ManagementNodeReadyExtensionPoint,
        ReportQuotaExtensionPoint, ResourceOwnerPreChangeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ImageManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private ImageDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    protected RESTFacade restf;

    private Map<String, ImageFactory> imageFactories = Collections.synchronizedMap(new HashMap<>());
    private static final Set<Class> allowedMessageAfterDeletion = new HashSet<>();
    private Future<Void> expungeTask;

    static {
        allowedMessageAfterDeletion.add(ImageDeletionMsg.class);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof ImageMessage) {
            passThrough((ImageMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIAddImageMsg) {
            handle((APIAddImageMsg) msg);
        } else if (msg instanceof APIListImageMsg) {
            handle((APIListImageMsg) msg);
        } else if (msg instanceof APISearchImageMsg) {
            handle((APISearchImageMsg) msg);
        } else if (msg instanceof APIGetImageMsg) {
            handle((APIGetImageMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
            handle((APICreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromVolumeSnapshotMsg) {
            handle((APICreateRootVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeMsg) {
            handle((APICreateDataVolumeTemplateFromVolumeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APICreateDataVolumeTemplateFromVolumeMsg msg) {
        final APICreateDataVolumeTemplateFromVolumeEvent evt = new APICreateDataVolumeTemplateFromVolumeEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-template-from-volume-%s", msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            List<BackupStorageInventory> backupStorage = new ArrayList<>();
            ImageVO image;
            long actualSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-actual-size-of-data-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
                        smsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                SyncVolumeSizeReply sr = reply.castReply();
                                actualSize = sr.getActualSize();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-image-in-database";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                        q.select(VolumeVO_.format, VolumeVO_.size);
                        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
                        Tuple t = q.findTuple();

                        String format = t.get(0, String.class);
                        long size = t.get(1, Long.class);

                        final ImageVO vo = new ImageVO();
                        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                        vo.setName(msg.getName());
                        vo.setDescription(msg.getDescription());
                        vo.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
                        vo.setMediaType(ImageMediaType.DataVolumeTemplate);
                        vo.setSize(size);
                        vo.setActualSize(actualSize);
                        vo.setState(ImageState.Enabled);
                        vo.setStatus(ImageStatus.Creating);
                        vo.setFormat(format);
                        vo.setUrl(String.format("volume://%s", msg.getVolumeUuid()));
                        image = dbf.persistAndRefresh(vo);

                        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), ImageVO.class);
                        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), ImageVO.class.getSimpleName());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (image != null) {
                            dbf.remove(image);
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "select-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final String zoneUuid = new Callable<String>() {
                            @Override
                            @Transactional(readOnly = true)
                            public String call() {
                                String sql = "select ps.zoneUuid" +
                                        " from PrimaryStorageVO ps, VolumeVO vol" +
                                        " where vol.primaryStorageUuid = ps.uuid" +
                                        " and vol.uuid = :volUuid";
                                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                                q.setParameter("volUuid", msg.getVolumeUuid());
                                return q.getSingleResult();
                            }
                        }.call();

                        if (msg.getBackupStorageUuids() == null) {
                            AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                            amsg.setRequiredZoneUuid(zoneUuid);
                            amsg.setSize(actualSize);
                            bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        backupStorage.add(((AllocateBackupStorageReply) reply).getInventory());
                                        trigger.next();
                                    } else {
                                        trigger.fail(errf.stringToOperationError("cannot find proper backup storage", reply.getError()));
                                    }
                                }
                            });
                        } else {
                            List<AllocateBackupStorageMsg> amsgs = CollectionUtils.transformToList(msg.getBackupStorageUuids(), new Function<AllocateBackupStorageMsg, String>() {
                                @Override
                                public AllocateBackupStorageMsg call(String arg) {
                                    AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                                    amsg.setRequiredZoneUuid(zoneUuid);
                                    amsg.setSize(actualSize);
                                    amsg.setBackupStorageUuid(arg);
                                    bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                                    return amsg;
                                }
                            });

                            bus.send(amsgs, new CloudBusListCallBack(trigger) {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    List<ErrorCode> errs = new ArrayList<>();
                                    for (MessageReply r : replies) {
                                        if (r.isSuccess()) {
                                            backupStorage.add(((AllocateBackupStorageReply) r).getInventory());
                                        } else {
                                            errs.add(r.getError());
                                        }
                                    }

                                    if (backupStorage.isEmpty()) {
                                        trigger.fail(errf.stringToOperationError(String.format("failed to allocate all backup storage[uuid:%s], a list of error: %s",
                                                msg.getBackupStorageUuids(), JSONObjectUtil.toJsonString(errs))));
                                    } else {
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!backupStorage.isEmpty()) {
                            List<ReturnBackupStorageMsg> rmsgs = CollectionUtils.transformToList(backupStorage, new Function<ReturnBackupStorageMsg, BackupStorageInventory>() {
                                @Override
                                public ReturnBackupStorageMsg call(BackupStorageInventory arg) {
                                    ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                    rmsg.setBackupStorageUuid(arg.getUuid());
                                    rmsg.setSize(actualSize);
                                    bus.makeLocalServiceId(rmsg, BackupStorageConstant.SERVICE_ID);
                                    return rmsg;
                                }
                            });

                            bus.send(rmsgs, new CloudBusListCallBack() {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply r : replies) {
                                        BackupStorageInventory bs = backupStorage.get(replies.indexOf(r));
                                        logger.warn(String.format("failed to return %s bytes to backup storage[uuid:%s]", acntMgr, bs.getUuid()));
                                    }
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-data-volume-template-from-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CreateDataVolumeTemplateFromDataVolumeMsg> cmsgs = CollectionUtils.transformToList(backupStorage, new Function<CreateDataVolumeTemplateFromDataVolumeMsg, BackupStorageInventory>() {
                            @Override
                            public CreateDataVolumeTemplateFromDataVolumeMsg call(BackupStorageInventory bs) {
                                CreateDataVolumeTemplateFromDataVolumeMsg cmsg = new CreateDataVolumeTemplateFromDataVolumeMsg();
                                cmsg.setVolumeUuid(msg.getVolumeUuid());
                                cmsg.setBackupStorageUuid(bs.getUuid());
                                cmsg.setImageUuid(image.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                                return cmsg;
                            }
                        });

                        bus.send(cmsgs, new CloudBusListCallBack(msg) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                int fail = 0;
                                String mdsum = null;
                                ErrorCode err = null;
                                String format = null;
                                for (MessageReply r : replies) {
                                    BackupStorageInventory bs = backupStorage.get(replies.indexOf(r));
                                    if (!r.isSuccess()) {
                                        logger.warn(String.format("failed to create data volume template from volume[uuid:%s] on backup storage[uuid:%s], %s",
                                                msg.getVolumeUuid(), bs.getUuid(), r.getError()));
                                        fail++;
                                        err = r.getError();
                                        continue;
                                    }

                                    CreateDataVolumeTemplateFromDataVolumeReply reply = r.castReply();
                                    ImageBackupStorageRefVO ref = new ImageBackupStorageRefVO();
                                    ref.setBackupStorageUuid(bs.getUuid());
                                    ref.setStatus(ImageStatus.Ready);
                                    ref.setImageUuid(image.getUuid());
                                    ref.setInstallPath(reply.getInstallPath());
                                    dbf.persist(ref);

                                    if (mdsum == null) {
                                        mdsum = reply.getMd5sum();
                                    }
                                    if (reply.getFormat() != null) {
                                        format = reply.getFormat();
                                    }
                                }

                                int backupStorageNum = msg.getBackupStorageUuids() == null ? 1 : msg.getBackupStorageUuids().size();

                                if (fail == backupStorageNum) {
                                    ErrorCode errCode = errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                                            String.format("failed to create data volume template from volume[uuid:%s] on all backup storage%s. See cause for one of errors",
                                                    msg.getVolumeUuid(), msg.getBackupStorageUuids()),
                                            err
                                    );

                                    trigger.fail(errCode);
                                } else {
                                    image = dbf.reload(image);
                                    if (format != null) {
                                        image.setFormat(format);
                                    }
                                    image.setMd5Sum(mdsum);
                                    image.setStatus(ImageStatus.Ready);
                                    image = dbf.updateAndRefresh(image);

                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(ImageInventory.valueOf(image));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(final APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        final APICreateRootVolumeTemplateFromVolumeSnapshotEvent evt = new APICreateRootVolumeTemplateFromVolumeSnapshotEvent(msg.getId());

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.format);
        q.add(VolumeSnapshotVO_.uuid, Op.EQ, msg.getSnapshotUuid());
        String format = q.findValue();

        final ImageVO vo = new ImageVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setSystem(msg.isSystem());
        vo.setDescription(msg.getDescription());
        vo.setPlatform(ImagePlatform.valueOf(msg.getPlatform()));
        vo.setGuestOsType(vo.getGuestOsType());
        vo.setStatus(ImageStatus.Creating);
        vo.setState(ImageState.Enabled);
        vo.setFormat(format);
        vo.setMediaType(ImageMediaType.RootVolumeTemplate);
        vo.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
        vo.setUrl(String.format("volumeSnapshot://%s", msg.getSnapshotUuid()));
        dbf.persist(vo);

        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), ImageVO.class);
        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), ImageVO.class.getSimpleName());

        SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
        sq.select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid);
        sq.add(VolumeSnapshotVO_.uuid, Op.EQ, msg.getSnapshotUuid());
        Tuple t = sq.findTuple();
        String volumeUuid = t.get(0, String.class);
        String treeUuid = t.get(1, String.class);

        List<CreateTemplateFromVolumeSnapshotMsg> cmsgs = msg.getBackupStorageUuids().stream().map(bsUuid -> {
            CreateTemplateFromVolumeSnapshotMsg cmsg = new CreateTemplateFromVolumeSnapshotMsg();
            cmsg.setSnapshotUuid(msg.getSnapshotUuid());
            cmsg.setImageUuid(vo.getUuid());
            cmsg.setVolumeUuid(volumeUuid);
            cmsg.setTreeUuid(treeUuid);
            cmsg.setBackupStorageUuid(bsUuid);
            String resourceUuid = volumeUuid != null ? volumeUuid : treeUuid;
            bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
            return cmsg;
        }).collect(Collectors.toList());


        List<Failure> failures = new ArrayList<>();
        AsyncLatch latch = new AsyncLatch(cmsgs.size(), new NoErrorCompletion(msg) {
            @Override
            public void done() {
                if (failures.size() == cmsgs.size()) {
                    // failed on all
                    ErrorCodeList error = errf.stringToOperationError(String.format("failed to create template from" +
                                    " the volume snapshot[uuid:%s] on backup storage[uuids:%s]", msg.getSnapshotUuid(),
                            msg.getBackupStorageUuids()), failures.stream().map(f -> f.error).collect(Collectors.toList()));
                    evt.setErrorCode(error);
                    dbf.remove(vo);
                } else {
                    ImageVO imvo = dbf.reload(vo);
                    evt.setInventory(ImageInventory.valueOf(imvo));

                    logger.debug(String.format("successfully created image[uuid:%s, name:%s] from volume snapshot[uuid:%s]",
                            imvo.getUuid(), imvo.getName(), msg.getSnapshotUuid()));
                }

                if (!failures.isEmpty()) {
                    evt.setFailuresOnBackupStorage(failures);
                }

                bus.publish(evt);
            }
        });

        RunOnce once = new RunOnce();
        for (CreateTemplateFromVolumeSnapshotMsg cmsg : cmsgs) {
            bus.send(cmsg, new CloudBusCallBack(latch) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        synchronized (failures) {
                            Failure failure = new Failure();
                            failure.error = reply.getError();
                            failure.backupStorageUuid = cmsg.getBackupStorageUuid();
                            failures.add(failure);
                        }
                    } else {
                        CreateTemplateFromVolumeSnapshotReply cr = reply.castReply();
                        ImageBackupStorageRefVO ref = new ImageBackupStorageRefVO();
                        ref.setBackupStorageUuid(cr.getBackupStorageUuid());
                        ref.setInstallPath(cr.getBackupStorageInstallPath());
                        ref.setStatus(ImageStatus.Ready);
                        ref.setImageUuid(vo.getUuid());
                        dbf.persist(ref);

                        once.run(() -> {
                            vo.setSize(cr.getSize());
                            vo.setActualSize(cr.getActualSize());
                            vo.setStatus(ImageStatus.Ready);
                            dbf.update(vo);
                        });
                    }

                    latch.ack();
                }
            });
        }
    }

    private void passThrough(ImageMessage msg) {
        ImageVO vo = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (vo == null && allowedMessageAfterDeletion.contains(msg.getClass())) {
            ImageEO eo = dbf.findByUuid(msg.getImageUuid(), ImageEO.class);
            vo = ObjectUtils.newAndCopy(eo, ImageVO.class);
        }

        if (vo == null) {
            String err = String.format("Cannot find image[uuid:%s], it may have been deleted", msg.getImageUuid());
            logger.warn(err);
            bus.replyErrorByMessageType((Message) msg, errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND, err));
            return;
        }

        ImageFactory factory = getImageFacotry(ImageType.valueOf(vo.getType()));
        Image img = factory.getImage(vo);
        img.handleMessage((Message) msg);
    }


    private void handle(final APICreateRootVolumeTemplateFromRootVolumeMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-root-volume-%s", msg.getRootVolumeUuid()));
        chain.then(new ShareFlow() {
            ImageVO imageVO;
            VolumeInventory rootVolume;
            Long imageActualSize;
            List<BackupStorageInventory> targetBackupStorages = new ArrayList<>();
            String zoneUuid;

            {
                VolumeVO rootvo = dbf.findByUuid(msg.getRootVolumeUuid(), VolumeVO.class);
                rootVolume = VolumeInventory.valueOf(rootvo);

                SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
                q.select(PrimaryStorageVO_.zoneUuid);
                q.add(PrimaryStorageVO_.uuid, Op.EQ, rootVolume.getPrimaryStorageUuid());
                zoneUuid = q.findValue();
            }


            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-volume-actual-size";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
                        msg.setVolumeUuid(rootVolume.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, rootVolume.getPrimaryStorageUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                SyncVolumeSizeReply sr = reply.castReply();
                                imageActualSize = sr.getActualSize();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-image-in-database";

                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                        q.add(VolumeVO_.uuid, Op.EQ, msg.getRootVolumeUuid());
                        final VolumeVO volvo = q.find();

                        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(volvo.getUuid());

                        final ImageVO imvo = new ImageVO();
                        if (msg.getResourceUuid() != null) {
                            imvo.setUuid(msg.getResourceUuid());
                        } else {
                            imvo.setUuid(Platform.getUuid());
                        }
                        imvo.setDescription(msg.getDescription());
                        imvo.setMediaType(ImageMediaType.RootVolumeTemplate);
                        imvo.setState(ImageState.Enabled);
                        imvo.setGuestOsType(msg.getGuestOsType());
                        imvo.setFormat(volvo.getFormat());
                        imvo.setName(msg.getName());
                        imvo.setSystem(msg.isSystem());
                        imvo.setPlatform(ImagePlatform.valueOf(msg.getPlatform()));
                        imvo.setStatus(ImageStatus.Downloading);
                        imvo.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
                        imvo.setUrl(String.format("volume://%s", msg.getRootVolumeUuid()));
                        imvo.setSize(volvo.getSize());
                        imvo.setActualSize(imageActualSize);

                        dbf.persist(imvo);

                        acntMgr.createAccountResourceRef(accountUuid, imvo.getUuid(), ImageVO.class);
                        tagMgr.createTagsFromAPICreateMessage(msg, imvo.getUuid(), ImageVO.class.getSimpleName());

                        imageVO = imvo;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (imageVO != null) {
                            dbf.remove(imageVO);
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = String.format("select-backup-storage");

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (msg.getBackupStorageUuids() == null) {
                            AllocateBackupStorageMsg abmsg = new AllocateBackupStorageMsg();
                            abmsg.setRequiredZoneUuid(zoneUuid);
                            abmsg.setSize(imageActualSize);
                            bus.makeLocalServiceId(abmsg, BackupStorageConstant.SERVICE_ID);

                            bus.send(abmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        targetBackupStorages.add(((AllocateBackupStorageReply) reply).getInventory());
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        } else {
                            List<AllocateBackupStorageMsg> amsgs = CollectionUtils.transformToList(msg.getBackupStorageUuids(), new Function<AllocateBackupStorageMsg, String>() {
                                @Override
                                public AllocateBackupStorageMsg call(String arg) {
                                    AllocateBackupStorageMsg abmsg = new AllocateBackupStorageMsg();
                                    abmsg.setSize(imageActualSize);
                                    abmsg.setBackupStorageUuid(arg);
                                    bus.makeLocalServiceId(abmsg, BackupStorageConstant.SERVICE_ID);
                                    return abmsg;
                                }
                            });

                            bus.send(amsgs, new CloudBusListCallBack(trigger) {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    List<ErrorCode> errs = new ArrayList<>();
                                    for (MessageReply r : replies) {
                                        if (r.isSuccess()) {
                                            targetBackupStorages.add(((AllocateBackupStorageReply) r).getInventory());
                                        } else {
                                            errs.add(r.getError());
                                        }
                                    }

                                    if (targetBackupStorages.isEmpty()) {
                                        trigger.fail(errf.stringToOperationError(String.format("unable to allocate backup storage specified by uuids%s, list errors are: %s",
                                                msg.getBackupStorageUuids(), JSONObjectUtil.toJsonString(errs))));
                                    } else {
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (targetBackupStorages.isEmpty()) {
                            trigger.rollback();
                            return;
                        }

                        List<ReturnBackupStorageMsg> rmsgs = CollectionUtils.transformToList(targetBackupStorages, new Function<ReturnBackupStorageMsg, BackupStorageInventory>() {
                            @Override
                            public ReturnBackupStorageMsg call(BackupStorageInventory arg) {
                                ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                rmsg.setBackupStorageUuid(arg.getUuid());
                                rmsg.setSize(imageActualSize);
                                bus.makeLocalServiceId(rmsg, BackupStorageConstant.SERVICE_ID);
                                return rmsg;
                            }
                        });

                        bus.send(rmsgs, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                for (MessageReply r : replies) {
                                    if (!r.isSuccess()) {
                                        BackupStorageInventory bs = targetBackupStorages.get(replies.indexOf(r));
                                        logger.warn(String.format("failed to return capacity[%s] to backup storage[uuid:%s], because %s",
                                                imageActualSize, bs.getUuid(), r.getError()));
                                    }
                                }

                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("start-creating-template");

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CreateTemplateFromVmRootVolumeMsg> cmsgs = CollectionUtils.transformToList(targetBackupStorages, new Function<CreateTemplateFromVmRootVolumeMsg, BackupStorageInventory>() {
                            @Override
                            public CreateTemplateFromVmRootVolumeMsg call(BackupStorageInventory arg) {
                                CreateTemplateFromVmRootVolumeMsg cmsg = new CreateTemplateFromVmRootVolumeMsg();
                                cmsg.setRootVolumeInventory(rootVolume);
                                cmsg.setBackupStorageUuid(arg.getUuid());
                                cmsg.setImageInventory(ImageInventory.valueOf(imageVO));
                                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, rootVolume.getVmInstanceUuid());
                                return cmsg;
                            }
                        });

                        bus.send(cmsgs, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                boolean success = false;
                                ErrorCode err = null;

                                for (MessageReply r : replies) {
                                    BackupStorageInventory bs = targetBackupStorages.get(replies.indexOf(r));

                                    if (!r.isSuccess()) {
                                        logger.warn(String.format("failed to create image from root volume[uuid:%s] on backup storage[uuid:%s], because %s",
                                                msg.getRootVolumeUuid(), bs.getUuid(), r.getError()));
                                        err = r.getError();
                                        continue;
                                    }

                                    CreateTemplateFromVmRootVolumeReply reply = (CreateTemplateFromVmRootVolumeReply) r;
                                    ImageBackupStorageRefVO ref = new ImageBackupStorageRefVO();
                                    ref.setBackupStorageUuid(bs.getUuid());
                                    ref.setStatus(ImageStatus.Ready);
                                    ref.setImageUuid(imageVO.getUuid());
                                    ref.setInstallPath(reply.getInstallPath());
                                    dbf.persist(ref);

                                    imageVO.setStatus(ImageStatus.Ready);
                                    if (reply.getFormat() != null) {
                                        imageVO.setFormat(reply.getFormat());
                                    }
                                    dbf.update(imageVO);
                                    imageVO = dbf.reload(imageVO);
                                    success = true;
                                    logger.debug(String.format("successfully created image[uuid:%s] from root volume[uuid:%s] on backup storage[uuid:%s]",
                                            imageVO.getUuid(), msg.getRootVolumeUuid(), bs.getUuid()));
                                }

                                if (success) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, String.format("failed to create image from root volume[uuid:%s] on all backup storage, see cause for one of errors",
                                            msg.getRootVolumeUuid()), err));
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        APICreateRootVolumeTemplateFromRootVolumeEvent evt = new APICreateRootVolumeTemplateFromRootVolumeEvent(msg.getId());
                        imageVO = dbf.reload(imageVO);
                        ImageInventory iinv = ImageInventory.valueOf(imageVO);
                        evt.setInventory(iinv);
                        logger.warn(String.format("successfully create template[uuid:%s] from root volume[uuid:%s]", iinv.getUuid(), msg.getRootVolumeUuid()));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        APICreateRootVolumeTemplateFromRootVolumeEvent evt = new APICreateRootVolumeTemplateFromRootVolumeEvent(msg.getId());
                        evt.setErrorCode(errCode);
                        logger.warn(String.format("failed to create template from root volume[uuid:%s], because %s", msg.getRootVolumeUuid(), errCode));
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIGetImageMsg msg) {
        SearchQuery<ImageInventory> sq = new SearchQuery(ImageInventory.class);
        sq.addAccountAsAnd(msg);
        sq.add("uuid", SearchOp.AND_EQ, msg.getUuid());
        List<ImageInventory> invs = sq.list();
        APIGetImageReply reply = new APIGetImageReply();
        if (!invs.isEmpty()) {
            reply.setInventory(JSONObjectUtil.toJsonString(invs.get(0)));
        }
        bus.reply(msg, reply);
    }

    private void handle(APISearchImageMsg msg) {
        SearchQuery<ImageInventory> sq = SearchQuery.create(msg, ImageInventory.class);
        sq.addAccountAsAnd(msg);
        String content = sq.listAsString();
        APISearchImageReply reply = new APISearchImageReply();
        reply.setContent(content);
        bus.reply(msg, reply);
    }

    private void handle(APIListImageMsg msg) {
        List<ImageVO> vos = dbf.listAll(ImageVO.class);
        List<ImageInventory> invs = ImageInventory.valueOf(vos);
        APIListImageReply reply = new APIListImageReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    @Deferred
    private void handle(final APIAddImageMsg msg) {
        String imageType = msg.getType();
        imageType = imageType == null ? DefaultImageFactory.type.toString() : imageType;

        final APIAddImageEvent evt = new APIAddImageEvent(msg.getId());
        ImageVO vo = new ImageVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        if (msg.getFormat().equals(ImageConstant.ISO_FORMAT_STRING)) {
            vo.setMediaType(ImageMediaType.ISO);
        } else {
            vo.setMediaType(ImageMediaType.valueOf(msg.getMediaType()));
        }
        vo.setType(imageType);
        vo.setSystem(msg.isSystem());
        vo.setGuestOsType(msg.getGuestOsType());
        vo.setFormat(msg.getFormat());
        vo.setStatus(ImageStatus.Downloading);
        vo.setState(ImageState.Enabled);
        vo.setUrl(msg.getUrl());
        vo.setDescription(msg.getDescription());
        vo.setPlatform(ImagePlatform.valueOf(msg.getPlatform()));

        ImageFactory factory = getImageFacotry(ImageType.valueOf(imageType));
        final ImageVO ivo = factory.createImage(vo, msg);

        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), ImageVO.class);
        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), ImageVO.class.getSimpleName());

        Defer.guard(() -> dbf.remove(ivo));

        final ImageInventory inv = ImageInventory.valueOf(ivo);
        for (AddImageExtensionPoint ext : pluginRgty.getExtensionList(AddImageExtensionPoint.class)) {
            ext.preAddImage(inv);
        }

        final List<DownloadImageMsg> dmsgs = CollectionUtils.transformToList(msg.getBackupStorageUuids(), new Function<DownloadImageMsg, String>() {
            @Override
            public DownloadImageMsg call(String arg) {
                boolean enableInject = false;
                if(msg.getSystemTags() != null)
                    if (msg.getSystemTags().contains(ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat()) == true)
                        enableInject = ImageGlobalConfig.ENABLE_QEMUGA.value(Boolean.class);
                DownloadImageMsg dmsg = new DownloadImageMsg(inv, enableInject);
                dmsg.setBackupStorageUuid(arg);
                dmsg.setFormat(msg.getFormat());
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, arg);
                return dmsg;
            }
        });

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class), new ForEachFunction<AddImageExtensionPoint>() {
            @Override
            public void run(AddImageExtensionPoint ext) {
                ext.beforeAddImage(inv);
            }
        });

        new LoopAsyncBatch<DownloadImageMsg>(msg) {
            AtomicBoolean success = new AtomicBoolean(false);

            @Override
            protected Collection<DownloadImageMsg> collect() {
                return dmsgs;
            }

            @Override
            protected AsyncBatchRunner forEach(DownloadImageMsg dmsg) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        ImageBackupStorageRefVO ref = new ImageBackupStorageRefVO();
                        ref.setImageUuid(ivo.getUuid());
                        ref.setInstallPath("");
                        ref.setBackupStorageUuid(dmsg.getBackupStorageUuid());
                        ref.setStatus(ImageStatus.Downloading);
                        dbf.persist(ref);

                        logger.debug(String.format("handle dmsg, dmsg.isInject() is: %s", String.valueOf(dmsg.isInject())));
                        bus.send(dmsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    errors.add(reply.getError());
                                    dbf.remove(ref);
                                } else {
                                    DownloadImageReply re = reply.castReply();
                                    ref.setStatus(ImageStatus.Ready);
                                    ref.setInstallPath(re.getInstallPath());
                                    dbf.update(ref);
                                    if(re.isInjected()) {
                                        logger.debug(String.format("ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat() is: %s", ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat()));
                                        new ImageAgentOperate().attachAgentToImage(dmsg.getImageInventory().getUuid(), ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat());
                                    }

                                    if (success.compareAndSet(false, true)) {
                                        ivo.setMd5Sum(re.getMd5sum());
                                        ivo.setSize(re.getSize());
                                        ivo.setActualSize(re.getActualSize());
                                        ivo.setStatus(ImageStatus.Ready);
                                        ivo.setFormat(re.getFormat());
                                        dbf.update(ivo);
                                    }

                                    logger.debug(String.format("successfully downloaded image[uuid:%s, name:%s] to backup storage[uuid:%s]",
                                            inv.getUuid(), inv.getName(), dmsg.getBackupStorageUuid()));
                                }

                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                // TODO: check if the database still has the record of the image
                // if there is no record, that means user delete the image during the downloading,
                // then we need to cleanup

                if (success.get()) {
                    ImageVO vo = dbf.reload(ivo);
                    final ImageInventory einv = ImageInventory.valueOf(vo);

                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class), new ForEachFunction<AddImageExtensionPoint>() {
                        @Override
                        public void run(AddImageExtensionPoint ext) {
                            ext.afterAddImage(einv);
                        }
                    });

                    evt.setInventory(einv);
                } else {
                    final ErrorCode err = errf.instantiateErrorCode(SysErrors.CREATE_RESOURCE_ERROR, String.format("Failed to download image[name:%s] on all backup storage%s.",
                            inv.getName(), msg.getBackupStorageUuids()), errors);

                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class), new ForEachFunction<AddImageExtensionPoint>() {
                        @Override
                        public void run(AddImageExtensionPoint ext) {
                            ext.failedToAddImage(inv, err);
                        }
                    });

                    dbf.remove(ivo);
                    evt.setErrorCode(err);
                }

                bus.publish(evt);
            }
        }.start();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ImageConstant.SERVICE_ID);
    }

    private void populateExtensions() {
        for (ImageFactory f : pluginRgty.getExtensionList(ImageFactory.class)) {
            ImageFactory old = imageFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ImageFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getType()));
            }
            imageFactories.put(f.getType().toString(), f);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        installGlobalConfigUpdater();
        return true;
    }

    private void installGlobalConfigUpdater() {
        ImageGlobalConfig.DELETION_POLICY.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startExpungeTask();
            }
        });
        ImageGlobalConfig.EXPUNGE_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startExpungeTask();
            }
        });
        ImageGlobalConfig.EXPUNGE_PERIOD.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startExpungeTask();
            }
        });
    }

    private void startExpungeTask() {
        if (expungeTask != null) {
            expungeTask.cancel(true);
        }

        expungeTask = thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            private List<Tuple> getDeletedImageManagedByUs() {
                int qun = 1000;
                SimpleQuery q = dbf.createQuery(ImageBackupStorageRefVO.class);
                q.add(ImageBackupStorageRefVO_.status, Op.EQ, ImageStatus.Deleted);
                long amount = q.count();
                int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
                int start = 0;
                List<Tuple> ret = new ArrayList<Tuple>();
                for (int i = 0; i < times; i++) {
                    q = dbf.createQuery(ImageBackupStorageRefVO.class);
                    q.select(ImageBackupStorageRefVO_.imageUuid, ImageBackupStorageRefVO_.lastOpDate, ImageBackupStorageRefVO_.backupStorageUuid);
                    q.add(ImageBackupStorageRefVO_.status, Op.EQ, ImageStatus.Deleted);
                    q.setLimit(qun);
                    q.setStart(start);
                    List<Tuple> ts = q.listTuple();
                    start += qun;

                    for (Tuple t : ts) {
                        String imageUuid = t.get(0, String.class);
                        if (!destMaker.isManagedByUs(imageUuid)) {
                            continue;
                        }
                        ret.add(t);
                    }
                }

                return ret;
            }

            @Override
            public boolean run() {
                final List<Tuple> images = getDeletedImageManagedByUs();
                if (images.isEmpty()) {
                    logger.debug("[Image Expunge Task]: no images to expunge");
                    return false;
                }

                for (Tuple t : images) {
                    String imageUuid = t.get(0, String.class);
                    Timestamp date = t.get(1, Timestamp.class);
                    String bsUuid = t.get(2, String.class);

                    final Timestamp current = dbf.getCurrentSqlTime();
                    if (current.getTime() >= date.getTime() + TimeUnit.SECONDS.toMillis(ImageGlobalConfig.EXPUNGE_PERIOD.value(Long.class))) {
                        ImageDeletionPolicy deletionPolicy = deletionPolicyMgr.getDeletionPolicy(imageUuid);
                        if (ImageDeletionPolicy.Never == deletionPolicy) {
                            logger.debug(String.format("the deletion policy[Never] is set for the image[uuid:%s] on the backup storage[uuid:%s]," +
                                    "don't expunge it", images, bsUuid));
                            continue;
                        }

                        ExpungeImageMsg msg = new ExpungeImageMsg();
                        msg.setImageUuid(imageUuid);
                        msg.setBackupStorageUuid(bsUuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imageUuid);
                        bus.send(msg, new CloudBusCallBack() {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    //TODO
                                    logger.warn(String.format("failed to expunge the image[uuid:%s], %s", images, reply.getError()));
                                }
                            }
                        });
                    }
                }

                return false;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return ImageGlobalConfig.EXPUNGE_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "expunge-image";
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    private ImageFactory getImageFacotry(ImageType type) {
        ImageFactory factory = imageFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Unable to find ImageFactory with type[%s]", type));
        }
        return factory;
    }

    @Override
    public void managementNodeReady() {
        startExpungeTask();
    }

    @Override
    public List<Quota> reportQuota() {
        Quota.QuotaOperator checker = new Quota.QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, Quota.QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APIAddImageMsg) {
                        check((APIAddImageMsg) msg, pairs);
                    } else if (msg instanceof APIRecoverImageMsg) {
                        check((APIRecoverImageMsg) msg, pairs);
                    } else if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                } else {
                    if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, Quota.QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                List<Quota.QuotaUsage> usages = new ArrayList<>();

                ImageQuotaUtil.ImageQuota imageQuota = new ImageQuotaUtil().getUsed(accountUuid);

                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(ImageConstant.QUOTA_IMAGE_NUM);
                usage.setUsed(imageQuota.imageNum);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(ImageConstant.QUOTA_IMAGE_SIZE);
                usage.setUsed(imageQuota.imageSize);
                usages.add(usage);

                return usages;
            }

            @Transactional(readOnly = true)
            private void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
                if (new QuotaUtil().isAdminAccount(resourceTargetOwnerAccountUuid)) {
                    return;
                }

                SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
                q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, msg.getResourceUuid());
                AccountResourceRefVO accResRefVO = q.find();


                if (accResRefVO.getResourceType().equals(ImageVO.class.getSimpleName())) {
                    long imageNumQuota = pairs.get(ImageConstant.QUOTA_IMAGE_NUM).getValue();
                    long imageSizeQuota = pairs.get(ImageConstant.QUOTA_IMAGE_SIZE).getValue();

                    long imageNumUsed = new ImageQuotaUtil().getUsedImageNum(resourceTargetOwnerAccountUuid);
                    long imageSizeUsed = new ImageQuotaUtil().getUsedImageSize(resourceTargetOwnerAccountUuid);

                    ImageVO image = dbf.getEntityManager().find(ImageVO.class, msg.getResourceUuid());
                    long imageNumAsked = 1;
                    long imageSizeAsked = image.getSize();


                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    {
                        quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                        quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                        quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                        quotaCompareInfo.quotaName = ImageConstant.QUOTA_IMAGE_NUM;
                        quotaCompareInfo.quotaValue = imageNumQuota;
                        quotaCompareInfo.currentUsed = imageNumUsed;
                        quotaCompareInfo.request = imageNumAsked;
                        new QuotaUtil().CheckQuota(quotaCompareInfo);
                    }

                    {
                        quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                        quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                        quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                        quotaCompareInfo.quotaName = ImageConstant.QUOTA_IMAGE_SIZE;
                        quotaCompareInfo.quotaValue = imageSizeQuota;
                        quotaCompareInfo.currentUsed = imageSizeUsed;
                        quotaCompareInfo.request = imageSizeAsked;
                        new QuotaUtil().CheckQuota(quotaCompareInfo);
                    }
                }

            }

            @Transactional(readOnly = true)
            private void check(APIRecoverImageMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getImageUuid());

                long imageNumQuota = pairs.get(ImageConstant.QUOTA_IMAGE_NUM).getValue();
                long imageSizeQuota = pairs.get(ImageConstant.QUOTA_IMAGE_SIZE).getValue();
                long imageNumUsed = new ImageQuotaUtil().getUsedImageNum(resourceTargetOwnerAccountUuid);
                long imageSizeUsed = new ImageQuotaUtil().getUsedImageSize(resourceTargetOwnerAccountUuid);

                ImageVO image = dbf.getEntityManager().find(ImageVO.class, msg.getImageUuid());
                long imageNumAsked = 1;
                long imageSizeAsked = image.getSize();

                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = ImageConstant.QUOTA_IMAGE_NUM;
                    quotaCompareInfo.quotaValue = imageNumQuota;
                    quotaCompareInfo.currentUsed = imageNumUsed;
                    quotaCompareInfo.request = imageNumAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }

                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = ImageConstant.QUOTA_IMAGE_SIZE;
                    quotaCompareInfo.quotaValue = imageSizeQuota;
                    quotaCompareInfo.currentUsed = imageSizeUsed;
                    quotaCompareInfo.request = imageSizeAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }

            @Transactional(readOnly = true)
            private void check(APIAddImageMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();
                long imageNumQuota = pairs.get(ImageConstant.QUOTA_IMAGE_NUM).getValue();
                long imageNumUsed = new ImageQuotaUtil().getUsedImageNum(resourceTargetOwnerAccountUuid);
                long imageNumAsked = 1;

                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = ImageConstant.QUOTA_IMAGE_NUM;
                    quotaCompareInfo.quotaValue = imageNumQuota;
                    quotaCompareInfo.currentUsed = imageNumUsed;
                    quotaCompareInfo.request = imageNumAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
                new ImageQuotaUtil().checkImageSizeQuotaUseHttpHead(msg, pairs);
            }
        };

        Quota quota = new Quota();
        quota.setOperator(checker);
        quota.addMessageNeedValidation(APIAddImageMsg.class);
        quota.addMessageNeedValidation(APIRecoverImageMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);

        Quota.QuotaPair p = new Quota.QuotaPair();
        p.setName(ImageConstant.QUOTA_IMAGE_NUM);
        p.setValue(20);
        quota.addPair(p);

        p = new Quota.QuotaPair();
        p.setName(ImageConstant.QUOTA_IMAGE_SIZE);
        p.setValue(SizeUnit.TERABYTE.toByte(10));
        quota.addPair(p);

        return list(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public void resourceOwnerPreChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        String resourceUuid = ref.getResourceUuid();
        // skip for admin
        if (new QuotaUtil().isAdminAccount(newOwnerUuid)) {
            return;
        }

        SimpleQuery<AccountResourceRefVO> sq = dbf.createQuery(AccountResourceRefVO.class);
        sq.add(AccountResourceRefVO_.resourceUuid, Op.EQ, resourceUuid);
        AccountResourceRefVO accResRefVO = sq.find();
        // skip if not vm instance
        if (!accResRefVO.getResourceType().equals(VmInstanceVO.class.getSimpleName())) {
            return;
        }
        // image
        VmInstanceVO vmInstanceVO = dbf.findByUuid(resourceUuid, VmInstanceVO.class);
        String isoUuid = VmSystemTags.ISO.getTokenByResourceUuid(resourceUuid, VmSystemTags.ISO_TOKEN);
        if (isoUuid == null || isoUuid.equals("")) {
            return;
        }
        if (vmInstanceVO.getImageUuid() != null && !vmInstanceVO.getImageUuid().equals("")) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("unable to change owner for vmInstance[uuid:%s]." +
                                    "ISO[uuid:%s] is not accessible to target account[uuid:%s]." +
                                    "You'd better share the image to target account or unmount it before change",
                            resourceUuid, isoUuid, newOwnerUuid)
            ));
        }
    }
}
