package org.zstack.image;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.*;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.core.*;
import org.zstack.header.core.progress.TaskProgressRange;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.*;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.longjob.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.CancelJobOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.CreateTemplateFromVmRootVolumeMsg;
import org.zstack.header.vm.CreateTemplateFromVmRootVolumeReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQL;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.core.progress.ProgressReportService.getTaskStage;
import static org.zstack.core.progress.ProgressReportService.reportProgress;
import static org.zstack.longjob.LongJobUtils.buildErrIfCanceled;
import static org.zstack.longjob.LongJobUtils.noncancelableErr;
import static org.zstack.utils.CollectionDSL.list;

public class ImageManagerImpl extends AbstractService implements ImageManager, ManagementNodeReadyExtensionPoint,
        ReportQuotaExtensionPoint, ResourceOwnerPreChangeExtensionPoint, HostAllocatorFilterExtensionPoint {
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
    @Autowired
    protected ImageExtensionPointEmitter extEmitter;



    private Map<String, ImageFactory> imageFactories = Collections.synchronizedMap(new HashMap<>());
    private List<ImageExtensionManager> imageExtensionManagers = new ArrayList<>();
    private static final Set<Class> allowedMessageAfterDeletion = new HashSet<>();
    private Future<Void> expungeTask;

    static {
        allowedMessageAfterDeletion.add(ImageDeletionMsg.class);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        ImageExtensionManager extensionManager = imageExtensionManagers.stream().filter(it -> it.getMessageClasses()
                .stream().anyMatch(clz -> clz.isAssignableFrom(msg.getClass()))).findFirst().orElse(null);
        if (extensionManager == null) {
            handleMessageBase(msg);
            return;
        }

        try {
            extensionManager.handleMessage(msg);
        } catch (StopRoutingException e) {
            handleMessageBase(msg);
        }
    }

    private void handleMessageBase(Message msg) {
        if (msg instanceof ImageMessage) {
            passThrough((ImageMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handle(AddImageMsg msg) {
        AddImageReply evt = new AddImageReply();
        AddImageLongJobData data = new AddImageLongJobData(msg);
        BeanUtils.copyProperties(msg, data);
        handleAddImageMsg(data, evt);
    }

    private void handle(CreateRootVolumeTemplateFromRootVolumeMsg msg) {
        CreateRootVolumeTemplateFromRootVolumeReply reply = new CreateRootVolumeTemplateFromRootVolumeReply();
        createRootVolumeTemplateFromRootVolume(msg, msg.getRootVolumeUuid(), new ReturnValueCompletion<ImageInventory>(msg) {
            @Override
            public void success(ImageInventory image) {
                reply.setInventory(image);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CreateRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        CreateRootVolumeTemplateFromVolumeSnapshotReply reply = new CreateRootVolumeTemplateFromVolumeSnapshotReply();
        createVolumeTemplateFromVolumeSnapshot(msg, msg.getSnapshotUuid(), new ReturnValueCompletion<ImageInventory>(msg) {
            @Override
            public void success(ImageInventory image) {
                reply.setInventory(image);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CreateTemporaryRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        CreateTemporaryRootVolumeTemplateFromVolumeSnapshotReply reply = new CreateTemporaryRootVolumeTemplateFromVolumeSnapshotReply();
        ImageMessageFiller.fillFromSnapshot(msg, msg.getSnapshotUuid());

        Tuple t = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid, VolumeSnapshotVO_.format)
                .eq(VolumeSnapshotVO_.uuid, msg.getSnapshotUuid()).findTuple();
        String volumeUuid = t.get(0, String.class);
        String treeUuid = t.get(1, String.class);
        String format = t.get(2, String.class);

        t = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid)
                .select(VolumeVO_.size, VolumeVO_.primaryStorageUuid)
                .findTuple();
        long size = t.get(0, Long.class);
        String volumePsUuid = t.get(1, String.class);

        ImageVO vo = createTemporaryImageInDb(msg, imgvo -> {
            imgvo.setSize(size);
            imgvo.setFormat(format);
            imgvo.setUrl(String.format("volumeSnapshot://%s", msg.getSnapshotUuid()));
        });

        createSysTag(msg, vo);

        CreateImageCacheFromVolumeSnapshotMsg cmsg = new CreateImageCacheFromVolumeSnapshotMsg();
        cmsg.setSnapshotUuid(msg.getSnapshotUuid());
        cmsg.setImageUuid(vo.getUuid());
        cmsg.setVolumeUuid(volumeUuid);
        cmsg.setTreeUuid(treeUuid);
        String resourceUuid = volumeUuid != null ? volumeUuid : treeUuid;
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                    bus.reply(msg, reply);
                    return;
                }

                CreateImageCacheFromVolumeSnapshotReply cr = r.castReply();

                vo.setActualSize(cr.getActualSize());
                vo.setStatus(ImageStatus.Ready);
                dbf.update(vo);
                reply.setInventory(ImageInventory.valueOf(vo));
                reply.setLocateHostUuid(cr.getLocationHostUuid());
                reply.setLocatePsUuid(volumePsUuid);

                bus.reply(msg, reply);
            }
        });
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AddImageMsg) {
            handle((AddImageMsg) msg);
        } else if (msg instanceof CreateRootVolumeTemplateFromRootVolumeMsg){
            handle((CreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof CancelCreateRootVolumeTemplateFromRootVolumeMsg) {
            handle((CancelCreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof CreateRootVolumeTemplateFromVolumeSnapshotMsg) {
            handle((CreateRootVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof CreateDataVolumeTemplateFromVolumeMsg){
            handle ((CreateDataVolumeTemplateFromVolumeMsg) msg);
        } else if (msg instanceof CreateTemporaryRootVolumeTemplateFromVolumeSnapshotMsg) {
            handle((CreateTemporaryRootVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof CreateTemporaryRootVolumeTemplateFromVolumeMsg) {
            handle((CreateTemporaryRootVolumeTemplateFromVolumeMsg) msg);
        } else if (msg instanceof CreateDataVolumeTemplateFromVolumeSnapshotMsg) {
            handle((CreateDataVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof CancelCreateDataVolumeTemplateFromVolumeMsg) {
            handle((CancelCreateDataVolumeTemplateFromVolumeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CreateDataVolumeTemplateFromVolumeMsg msg) {
        CreateDataVolumeTemplateFromVolumeReply reply = new CreateDataVolumeTemplateFromVolumeReply();
        createDataVolumeTemplateFromVolume(msg, msg.getVolumeUuid(), new ReturnValueCompletion<ImageInventory>(msg) {
            @Override
            public void success(ImageInventory image) {
                reply.setInventory(image);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CreateTemporaryRootVolumeTemplateFromVolumeMsg msg) {
        ImageMessageFiller.fillFromVolume(msg, msg.getVolumeUuid());

        CreateTemporaryRootVolumeTemplateFromVolumeReply reply = new CreateTemporaryRootVolumeTemplateFromVolumeReply();

        //tag::flow_check[]
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-temporary-volume-template-from-volume-%s", msg.getVolumeUuid()));
        chain.preCheck(data -> buildErrIfCanceled());
        //end::flow_check[]
        chain.then(new ShareFlow() {
            ImageVO image;
            long imageEstimateSize;
            String volumePsUuid;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-actual-size-of-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        EstimateVolumeTemplateSizeMsg smsg = new EstimateVolumeTemplateSizeMsg();
                        smsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                EstimateVolumeTemplateSizeReply sr = reply.castReply();
                                imageEstimateSize = sr.getActualSize();
                                volumePsUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid())
                                        .select(VolumeVO_.primaryStorageUuid)
                                        .findValue();
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

                        image = createTemporaryImageInDb(msg, vo -> {
                            vo.setSize(size);
                            vo.setActualSize(imageEstimateSize);
                            vo.setFormat(format);
                            vo.setUrl(String.format("volume://%s", msg.getVolumeUuid()));
                        });

                        createSysTag(msg, image);

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

                flow(new NoRollbackFlow() {
                    String __name__ = "create-image-cache-from-volume";
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateImageCacheFromVolumeMsg cmsg = new CreateImageCacheFromVolumeMsg();
                        cmsg.setUuid(msg.getVolumeUuid());
                        cmsg.setImage(ImageInventory.valueOf(image));
                        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, cmsg.getUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                    return;
                                }

                                CreateImageCacheFromVolumeReply cr = r.castReply();
                                reply.setLocateHostUuid(cr.getLocateHostUuid());
                                reply.setLocatePsUuid(volumePsUuid);
                                image = dbf.reload(image);
                                image.setStatus(ImageStatus.Ready);
                                image = dbf.updateAndRefresh(image);

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setInventory(ImageInventory.valueOf(image));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(CreateDataVolumeTemplateFromVolumeSnapshotMsg msg) {
        CreateDataVolumeTemplateFromVolumeSnapshotReply reply = new CreateDataVolumeTemplateFromVolumeSnapshotReply();
        createVolumeTemplateFromVolumeSnapshot(msg, msg.getSnapshotUuid(), new ReturnValueCompletion<ImageInventory>(msg) {
            @Override
            public void success(ImageInventory image) {
                reply.setInventory(image);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });

    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIAddImageMsg) {
            handle((APIAddImageMsg) msg);
        } else if (msg instanceof APIGetUploadImageJobDetailsMsg) {
            handle((APIGetUploadImageJobDetailsMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
            handle((APICreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromVolumeSnapshotMsg) {
            handle((APICreateRootVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeMsg) {
            handle((APICreateDataVolumeTemplateFromVolumeMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeSnapshotMsg) {
            handle((APICreateDataVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APICreateDataVolumeTemplateFromVolumeMsg msg) {
        APICreateDataVolumeTemplateFromVolumeEvent evt = new APICreateDataVolumeTemplateFromVolumeEvent(msg.getId());
        createDataVolumeTemplateFromVolume(msg, msg.getVolumeUuid(), new ReturnValueCompletion<ImageInventory>(evt) {
            @Override
            public void success(ImageInventory image) {
                evt.setInventory(image);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APICreateDataVolumeTemplateFromVolumeSnapshotMsg msg) {
        final APICreateDataVolumeTemplateFromVolumeSnapshotEvent evt = new APICreateDataVolumeTemplateFromVolumeSnapshotEvent(msg.getId());
        createVolumeTemplateFromVolumeSnapshot(msg, msg.getSnapshotUuid(), new ReturnValueCompletion<ImageInventory>(evt) {
            @Override
            public void success(ImageInventory image) {
                evt.setInventory(image);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        final APICreateRootVolumeTemplateFromVolumeSnapshotEvent evt = new APICreateRootVolumeTemplateFromVolumeSnapshotEvent(msg.getId());
        createVolumeTemplateFromVolumeSnapshot(msg, msg.getSnapshotUuid(), new ReturnValueCompletion<ImageInventory>(evt) {
            @Override
            public void success(ImageInventory image) {
                evt.setInventory(image);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void createVolumeTemplateFromVolumeSnapshot(AddImageMessage msg, String snapshotUuid, ReturnValueCompletion<ImageInventory> completion) {
        ImageMessageFiller.fillFromSnapshot(msg, snapshotUuid);

        Tuple t = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid, VolumeSnapshotVO_.format)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid).findTuple();
        String volumeUuid = t.get(0, String.class);
        String treeUuid = t.get(1, String.class);
        String format = t.get(2, String.class);

        ImageVO vo = createImageInDb(msg, imgvo -> {
            imgvo.setFormat(format);
            imgvo.setUrl(String.format("volumeSnapshot://%s", snapshotUuid));
        });

        createSysTag(msg, vo);

        if (msg.getBackupStorageUuids() == null || msg.getBackupStorageUuids().isEmpty()) {
            msg.setBackupStorageUuids(Collections.singletonList(null));
        }

        List<CreateTemplateFromVolumeSnapshotMsg> cmsgs = msg.getBackupStorageUuids().stream().map(bsUuid -> {
            CreateTemplateFromVolumeSnapshotMsg cmsg = new CreateTemplateFromVolumeSnapshotMsg();
            cmsg.setSnapshotUuid(snapshotUuid);
            cmsg.setImageUuid(vo.getUuid());
            cmsg.setVolumeUuid(volumeUuid);
            cmsg.setTreeUuid(treeUuid);
            cmsg.setBackupStorageUuid(bsUuid);
            String resourceUuid = volumeUuid != null ? volumeUuid : treeUuid;
            bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
            return cmsg;
        }).collect(Collectors.toList());

        List<APICreateRootVolumeTemplateFromVolumeSnapshotEvent.Failure> failures = new ArrayList<>();
        AsyncLatch latch = new AsyncLatch(cmsgs.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (failures.size() == cmsgs.size()) {
                    // failed on all
                    ErrorCodeList error = errf.stringToOperationError(String.format("failed to create template from" +
                                    " the volume snapshot[uuid:%s] on backup storage[uuids:%s]", snapshotUuid,
                            msg.getBackupStorageUuids()), failures.stream().map(f -> f.error).collect(Collectors.toList()));
                    dbf.remove(vo);
                    completion.fail(error);
                } else {
                    ImageVO imvo = dbf.reload(vo);
                    logger.debug(String.format("successfully created image[uuid:%s, name:%s] from volume snapshot[uuid:%s]",
                            imvo.getUuid(), imvo.getName(), snapshotUuid));

                    ImageInventory inv = ImageInventory.valueOf(imvo);
                    extEmitter.afterCreateImage(inv);
                    completion.success(inv);
                }
            }
        });

        String psUuid = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.primaryStorageUuid)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .findValue();

        RunOnce once = new RunOnce();
        for (CreateTemplateFromVolumeSnapshotMsg cmsg : cmsgs) {
            extEmitter.beforeCreateImage(ImageInventory.valueOf(vo), cmsg.getBackupStorageUuid(), psUuid);
            bus.send(cmsg, new CloudBusCallBack(latch) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        synchronized (failures) {
                            APICreateRootVolumeTemplateFromVolumeSnapshotEvent.Failure failure =
                                    new APICreateRootVolumeTemplateFromVolumeSnapshotEvent.Failure();
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

    @Override
    public ImageVO createImageInDb(AddImageMessage msg, Consumer<ImageVO> updater) {
        final ImageVO vo = new ImageVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setSystem(msg.isSystem());
        vo.setDescription(msg.getDescription());
        vo.setPlatform(msg.getPlatform() == null ? null : ImagePlatform.valueOf(msg.getPlatform()));
        vo.setGuestOsType(msg.getGuestOsType());
        vo.setVirtio(msg.isVirtio());
        vo.setArchitecture(msg.getArchitecture());
        vo.setStatus(ImageStatus.Creating);
        vo.setState(ImageState.Enabled);
        vo.setMediaType(ImageMediaType.valueOf(msg.getMediaType()));
        vo.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        updater.accept(vo);
        dbf.persist(vo);

        return vo;
    }

    private ImageVO createTemporaryImageInDb(AddImageMessage msg, Consumer<ImageVO> updater) {
        ImageVO image = createImageInDb(msg, updater);
        tagMgr.createNonInherentSystemTag(image.getUuid(),
                ImageSystemTags.TEMPORARY_IMAGE.getTagFormat(),
                ImageVO.class.getSimpleName());
        return image;
    }

    private void createSysTag(AddImageMessage msg, ImageVO vo) {
        if (msg instanceof APICreateMessage) {
            tagMgr.createTagsFromAPICreateMessage((APICreateMessage) msg, vo.getUuid(), ImageVO.class.getSimpleName());
        } else {
            tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), vo.getUuid(), ImageVO.class.getSimpleName());
        }
    }

    private void handle(final APIGetUploadImageJobDetailsMsg msg) {
        APIGetUploadImageJobDetailsReply reply = new APIGetUploadImageJobDetailsReply();

        String tag = ImageSystemTags.UPLOAD_IMAGE_INFO.instantiateTag(Collections.singletonMap(ImageSystemTags.IMAGE_ID, msg.getImageId()));
        List<String> longjobUuids = SQL.New("select job.uuid from SystemTagVO tag, LongJobVO job" +
                " where tag.tag = :tag" +
                " and tag.resourceUuid = job.uuid" +
                " and job.state not in :finalState", String.class)
                .param("tag", tag)
                .param("finalState", LongJobState.finalStates)
                .list();

        if (longjobUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        List jobs = ZQL.fromString(String.format("query longjob where uuid in ('%s')", String.join("','", longjobUuids)))
                .getSingleResultWithSession(msg.getSession()).inventories;
        for (Object j : jobs) {
            LongJobInventory job = (LongJobInventory) j;
            APIGetUploadImageJobDetailsReply.JobDetails detail = new APIGetUploadImageJobDetailsReply.JobDetails();
            detail.setLongJobUuid(job.getUuid());
            detail.setLongJobState(job.getState().toString());
            detail.setImageUuid(job.getTargetResourceUuid());

            ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                    .eq(ImageBackupStorageRefVO_.imageUuid, job.getTargetResourceUuid())
                    .limit(1)
                    .find();

            if (ref == null) {
                continue;
            }

            GetImageDownloadProgressMsg gmsg = new GetImageDownloadProgressMsg();
            gmsg.setBackupStorageUuid(ref.getBackupStorageUuid());
            gmsg.setImageUuid(ref.getImageUuid());
            try {
                String hostname = new URI(ref.getInstallPath()).getHost();
                gmsg.setHostname(hostname);
            } catch (URISyntaxException e) {
                continue;
            }

            bus.makeLocalServiceId(gmsg, BackupStorageConstant.SERVICE_ID);
            final MessageReply r = bus.call(gmsg);
            if (!r.isSuccess()) {
                continue;
            }

            GetImageDownloadProgressReply gr = r.castReply();
            detail.setOffset(gr.getDownloadSize());
            detail.setImageUploadUrl(ref.getInstallPath());
            reply.addExistingJobDetails(detail);
        }

        bus.reply(msg, reply);
    }

    private void passThrough(ImageMessage msg) {
        ImageVO vo = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (vo == null && allowedMessageAfterDeletion.contains(msg.getClass())) {
            ImageEO eo = dbf.findByUuid(msg.getImageUuid(), ImageEO.class);
            vo = ObjectUtils.newAndCopy(eo, ImageVO.class);
        }

        if (vo == null) {
            ErrorCode err = err(SysErrors.RESOURCE_NOT_FOUND, "Cannot find image[uuid:%s], it may have been deleted", msg.getImageUuid());
            logger.warn(err.getDetails());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        ImageFactory factory = getImageFacotry(ImageType.valueOf(vo.getType()));
        Image img = factory.getImage(vo);
        img.handleMessage((Message) msg);
    }


    private void handle(final APICreateRootVolumeTemplateFromRootVolumeMsg msg) {
        APICreateRootVolumeTemplateFromRootVolumeEvent evt = new APICreateRootVolumeTemplateFromRootVolumeEvent(msg.getId());
        createRootVolumeTemplateFromRootVolume(msg, msg.getRootVolumeUuid(), new ReturnValueCompletion<ImageInventory>(evt) {
            @Override
            public void success(ImageInventory image) {
                evt.setInventory(image);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private static boolean isUpload(final String url) {
        return url.startsWith("upload://");
    }

    private void handle(final APIAddImageMsg msg) {
        APIAddImageEvent evt = new APIAddImageEvent(msg.getId());
        AddImageLongJobData data = new AddImageLongJobData(msg);
        BeanUtils.copyProperties(msg, data);
        handleAddImageMsg(data, evt);
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

        imageExtensionManagers.addAll(pluginRgty.getExtensionList(ImageExtensionManager.class));
    }

    @Override
    public boolean start() {
        populateExtensions();
        installSystemTagValidator();
        installGlobalConfigUpdater();
        installGlobalConfigValidator();
        initDefaultImageArch();
        return true;
    }

    private void installSystemTagValidator() {
        installBootModeValidator();
    }

    private void installBootModeValidator() {
        class BootModeValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (!ImageSystemTags.BOOT_MODE.isMatch(systemTag)) {
                    return;
                }

                String bootMode = ImageSystemTags.BOOT_MODE.getTokenByTag(systemTag, ImageSystemTags.BOOT_MODE_TOKEN);
                validateBootMode(systemTag, bootMode);
            }

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
                    return;
                }

                int bootModeCount = 0;
                for (String systemTag : msg.getSystemTags()) {
                    if (ImageSystemTags.BOOT_MODE.isMatch(systemTag)) {
                        if (++bootModeCount > 1) {
                            throw new ApiMessageInterceptionException(argerr("only one bootMode system tag is allowed, but %d got", bootModeCount));
                        }

                        String bootMode = ImageSystemTags.BOOT_MODE.getTokenByTag(systemTag, ImageSystemTags.BOOT_MODE_TOKEN);
                        validateBootMode(systemTag, bootMode);
                    }
                }
            }

            private void validateBootMode(String systemTag, String bootMode) {
                boolean valid = false;
                for (ImageBootMode bm : ImageBootMode.values()) {
                    if (bm.name().equalsIgnoreCase(bootMode)) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    throw new ApiMessageInterceptionException(argerr(
                            "[%s] specified in system tag [%s] is not a valid boot mode", bootMode, systemTag)
                    );
                }
            }
        }

        BootModeValidator validator = new BootModeValidator();
        tagMgr.installCreateMessageValidator(ImageVO.class.getSimpleName(), validator);
        ImageSystemTags.BOOT_MODE.installValidator(validator);
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

    private void installGlobalConfigValidator() {
        ImageGlobalConfig.DOWNLOAD_LOCALPATH_CUSTOMFILTER.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                String[] usedList = newValue.split(";");
                boolean legal = Arrays.stream(usedList).allMatch(s ->
                        "blacklist".equals(s) ||
                                "whitelist".equals(s)
                );
                if (!legal) {
                    throw new GlobalConfigException(String.format("%s is not in [blacklist, whitelist]", newValue));
                }
            }
        });
        ImageGlobalConfig.DOWNLOAD_LOCALPATH_WHITELIST.installValidateExtension(getValidateExtension());
        ImageGlobalConfig.DOWNLOAD_LOCALPATH_BLACKLIST.installValidateExtension(getValidateExtension());
    }

    private GlobalConfigValidatorExtensionPoint getValidateExtension() {
        AntPathMatcher matcher = new AntPathMatcher();
        return new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (StringUtils.isBlank(newValue)) {
                    return;
                }
                String[] pathList = newValue.split(";");
                boolean legal = Arrays.stream(pathList).allMatch(s -> matcher.match("/**", s));
                if (!legal) {
                    throw new GlobalConfigException(String.format("invalid value: %s, path must separated by ';' and start with '/'", newValue));
                }
            }
        };
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
                        bus.send(msg, new CloudBusCallBack(null) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.debug(String.format("failed to expunge the image[uuid:%s] on the backup storage[uuid:%s], will try it later. %s",
                                            imageUuid, bsUuid, reply.getError()));
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

    private void initDefaultImageArch() {
        SQL.New(ImageVO.class)
                .isNull(ImageVO_.architecture)
                .notEq(ImageVO_.mediaType, ImageMediaType.DataVolumeTemplate)
                .set(ImageVO_.architecture, ImageArchitecture.defaultArch())
                .update();
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
                    } else if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    } else if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
                        check((APICreateRootVolumeTemplateFromRootVolumeMsg) msg, pairs);
                    } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeMsg) {
                        check((APICreateDataVolumeTemplateFromVolumeMsg) msg, pairs);
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
                usage.setName(ImageQuotaConstant.IMAGE_NUM);
                usage.setUsed(imageQuota.imageNum);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(ImageQuotaConstant.IMAGE_SIZE);
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
                    long imageNumQuota = pairs.get(ImageQuotaConstant.IMAGE_NUM).getValue();
                    long imageSizeQuota = pairs.get(ImageQuotaConstant.IMAGE_SIZE).getValue();

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
                        quotaCompareInfo.quotaName = ImageQuotaConstant.IMAGE_NUM;
                        quotaCompareInfo.quotaValue = imageNumQuota;
                        quotaCompareInfo.currentUsed = imageNumUsed;
                        quotaCompareInfo.request = imageNumAsked;
                        new QuotaUtil().CheckQuota(quotaCompareInfo);
                    }

                    {
                        quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                        quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                        quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                        quotaCompareInfo.quotaName = ImageQuotaConstant.IMAGE_SIZE;
                        quotaCompareInfo.quotaValue = imageSizeQuota;
                        quotaCompareInfo.currentUsed = imageSizeUsed;
                        quotaCompareInfo.request = imageSizeAsked;
                        new QuotaUtil().CheckQuota(quotaCompareInfo);
                    }
                }

            }

            @Transactional(readOnly = true)
            private void check(APIAddImageMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

                checkImageNumQuota(currentAccountUuid, resourceTargetOwnerAccountUuid, pairs);
                new ImageQuotaUtil().checkImageSizeQuotaUseHttpHead(msg, pairs);
            }

            @Transactional(readOnly = true)
            private void check(APICreateRootVolumeTemplateFromRootVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkImageNumQuota(msg.getSession().getAccountUuid(),
                        msg.getSession().getAccountUuid(),
                        pairs);

                Long templateSize = Q.New(VolumeVO.class)
                        .select(VolumeVO_.size)
                        .eq(VolumeVO_.uuid, msg.getRootVolumeUuid())
                        .findValue();

                checkImageSizeQuota(templateSize == null ? 0 : templateSize,
                        msg.getSession().getAccountUuid(),
                        msg.getSession().getAccountUuid(),
                        pairs);
            }

            @Transactional(readOnly = true)
            private void check(APICreateDataVolumeTemplateFromVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkImageNumQuota(msg.getSession().getAccountUuid(),
                        msg.getSession().getAccountUuid(),
                        pairs);

                Long templateSize = Q.New(VolumeVO.class)
                        .select(VolumeVO_.size)
                        .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                        .findValue();

                checkImageSizeQuota(templateSize == null ? 0 : templateSize,
                        msg.getSession().getAccountUuid(),
                        msg.getSession().getAccountUuid(),
                        pairs);
            }

            @Transactional(readOnly = true)
            private void checkImageSizeQuota(long requiredImageSize,
                                             String currentAccountUuid,
                                             String resourceTargetOwnerAccountUuid,
                                             Map<String, Quota.QuotaPair> pairs) {
                long imageSizeQuota = pairs.get(ImageQuotaConstant.IMAGE_SIZE).getValue();
                long imageSizeUsed = new ImageQuotaUtil().getUsedImageSize(resourceTargetOwnerAccountUuid);

                QuotaUtil.QuotaCompareInfo quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                quotaCompareInfo.quotaName = ImageQuotaConstant.IMAGE_SIZE;
                quotaCompareInfo.quotaValue = imageSizeQuota;
                quotaCompareInfo.currentUsed = imageSizeUsed;
                quotaCompareInfo.request = requiredImageSize;
                new QuotaUtil().CheckQuota(quotaCompareInfo);
            }

            @Transactional(readOnly = true)
            private void checkImageNumQuota(String currentAccountUuid,
                                            String resourceTargetOwnerAccountUuid,
                                            Map<String, Quota.QuotaPair> pairs) {
                long imageNumQuota = pairs.get(ImageQuotaConstant.IMAGE_NUM).getValue();
                long imageNumUsed = new ImageQuotaUtil().getUsedImageNum(resourceTargetOwnerAccountUuid);
                long imageNumAsked = 1;

                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = ImageQuotaConstant.IMAGE_NUM;
                    quotaCompareInfo.quotaValue = imageNumQuota;
                    quotaCompareInfo.currentUsed = imageNumUsed;
                    quotaCompareInfo.request = imageNumAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }
        };

        Quota quota = new Quota();
        quota.setOperator(checker);
        quota.addMessageNeedValidation(APIAddImageMsg.class);
        quota.addMessageNeedValidation(APIRecoverImageMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);
        quota.addMessageNeedValidation(APICreateRootVolumeTemplateFromRootVolumeMsg.class);
        quota.addMessageNeedValidation(APICreateDataVolumeTemplateFromVolumeMsg.class);

        Quota.QuotaPair p = new Quota.QuotaPair();
        p.setName(ImageQuotaConstant.IMAGE_NUM);
        p.setValue(ImageQuotaGlobalConfig.IMAGE_NUM.defaultValue(Long.class));
        quota.addPair(p);

        p = new Quota.QuotaPair();
        p.setName(ImageQuotaConstant.IMAGE_SIZE);
        p.setValue(ImageQuotaGlobalConfig.IMAGE_SIZE.defaultValue(Long.class));
        quota.addPair(p);

        return list(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public void resourceOwnerPreChange(AccountResourceRefInventory ref, String newOwnerUuid) {

    }

    private void saveRefVOByBsInventorys(List<BackupStorageInventory> inventorys, String imageUuid) {
        List<ImageBackupStorageRefVO> refs = new ArrayList<>();
        for (BackupStorageInventory backupStorageInventory : inventorys) {
            ImageBackupStorageRefVO ref = new ImageBackupStorageRefVO();
            ref.setBackupStorageUuid(backupStorageInventory.getUuid());
            ref.setStatus(ImageStatus.Creating);
            ref.setImageUuid(imageUuid);
            ref.setInstallPath("");
            refs.add(ref);
        }
        dbf.persistCollection(refs);
    }

    @Deferred
    private void handleAddImageMsg(AddImageLongJobData msgData, Message evt) {
        class InnerEvent extends Message {
            ErrorCode error;
            ImageInventory inv;

            void reply(Message reply) {
                if (evt instanceof APIAddImageEvent) {
                    APIAddImageEvent event = (APIAddImageEvent) reply;
                    if (null != error) {
                        event.setError(error);
                    }
                    if (null != inv) {
                        event.setInventory(inv);
                    }
                    bus.publish(event);
                } else if (evt instanceof AddImageReply) {
                    AddImageReply reply1 = (AddImageReply) reply;
                    if (null != error ){
                        reply1.setError(error);
                    }
                    if (null != inv) {
                        reply1.setInventory(inv);
                    }
                    bus.reply(msgData.getNeedReplyMessage(), reply1);
                }
            }
        }
        String accountUuid = msgData.getSession().getAccountUuid();
        String imageType = msgData.getType();
        imageType = imageType == null ? DefaultImageFactory.type.toString() : imageType;

        ImageVO vo = new ImageVO();
        if (msgData.getResourceUuid() != null) {
            dbf.eoCleanup(ImageVO.class, msgData.getResourceUuid());
            vo.setUuid(msgData.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msgData.getName());
        vo.setDescription(msgData.getDescription());
        if (msgData.getFormat().equals(ImageConstant.ISO_FORMAT_STRING)) {
            vo.setMediaType(ImageMediaType.ISO);
        } else {
            vo.setMediaType(ImageMediaType.valueOf(msgData.getMediaType()));
        }
        vo.setType(imageType);
        vo.setSystem(msgData.isSystem());
        vo.setGuestOsType(msgData.getGuestOsType());
        vo.setFormat(msgData.getFormat());
        vo.setStatus(ImageStatus.Downloading);
        vo.setState(ImageState.Enabled);
        vo.setUrl(msgData.getUrl());
        vo.setDescription(msgData.getDescription());
        vo.setVirtio(msgData.isVirtio());
        if (msgData.getFormat().equals(ImageConstant.VMTX_FORMAT_STRING)) {
            vo.setArchitecture(ImageArchitecture.x86_64.toString());
        } else {
            vo.setArchitecture(msgData.getArchitecture());
        }
        if (msgData.getPlatform() != null) {
            vo.setPlatform(ImagePlatform.valueOf(msgData.getPlatform()));
        }

        ImageFactory factory = getImageFacotry(ImageType.valueOf(imageType));
        final ImageVO ivo = new SQLBatchWithReturn<ImageVO>() {
            @Override
            protected ImageVO scripts() {
                vo.setAccountUuid(accountUuid);
                final ImageVO ivo = factory.createImage(vo);

                if (msgData.getNeedReplyMessage() instanceof APICreateMessage) {
                    tagMgr.createTagsFromAPICreateMessage((APICreateMessage) msgData.getNeedReplyMessage() , vo.getUuid(), ImageVO.class.getSimpleName());
                } else {
                    tagMgr.createTags(msgData.getSystemTags(), msgData.getUserTags(), vo.getUuid(), ImageVO.class.getSimpleName());
                }

                return ivo;
            }
        }.execute();

        List<ImageBackupStorageRefVO> refs = new ArrayList<>();
        for (String uuid : msgData.getBackupStorageUuids()) {
            ImageBackupStorageRefVO ref = new ImageBackupStorageRefVO();
            ref.setInstallPath("");
            ref.setBackupStorageUuid(uuid);
            ref.setStatus(ImageStatus.Downloading);
            ref.setImageUuid(ivo.getUuid());
            refs.add(ref);
        }
        dbf.persistCollection(refs);
        Defer.guard(() -> dbf.remove(ivo));

        final ImageInventory inv = ImageInventory.valueOf(ivo);

        extEmitter.preAddImage(inv);

        final List<DownloadImageMsg> dmsgs = CollectionUtils.transformToList(msgData.getBackupStorageUuids(), new Function<DownloadImageMsg, String>() {
            @Override
            public DownloadImageMsg call(String arg) {
                DownloadImageMsg dmsg = new DownloadImageMsg(inv);
                dmsg.setBackupStorageUuid(arg);
                dmsg.setFormat(msgData.getFormat());
                dmsg.setSystemTags(msgData.getSystemTags());
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, arg);
                return dmsg;
            }
        });

        extEmitter.beforeAddImage(inv);

        new LoopAsyncBatch<DownloadImageMsg>(msgData.getNeedReplyMessage()) {
            AtomicBoolean success = new AtomicBoolean(false);
            UploadImageTracker tracker = new UploadImageTracker();
            ConcurrentHashMap<String, AtomicBoolean> resultMap = new ConcurrentHashMap<>();

            @Override
            protected Collection<DownloadImageMsg> collect() {
                for (DownloadImageMsg dmsg : dmsgs) {
                    AtomicBoolean success = new AtomicBoolean(false);
                    resultMap.put(dmsg.getBackupStorageUuid(), success);
                }
                return dmsgs;
            }

            @Override
            protected AsyncBatchRunner forEach(DownloadImageMsg dmsg) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                                .eq(ImageBackupStorageRefVO_.imageUuid, ivo.getUuid())
                                .eq(ImageBackupStorageRefVO_.backupStorageUuid, dmsg.getBackupStorageUuid())
                                .find();
                        bus.send(dmsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    errors.add(reply.getError());
                                    dbf.remove(ref);
                                } else {
                                    DownloadImageReply re = reply.castReply();
                                    ref.setInstallPath(re.getInstallPath());

                                    if (isUpload(msgData.getUrl())) {
                                        tracker.addTrackTask(ivo, ref);
                                    } else {
                                        ref.setStatus(ImageStatus.Ready);
                                    }

                                    if (dbf.reload(ref) == null) {
                                        logger.debug(String.format("image[uuid: %s] has been deleted", ref.getImageUuid()));
                                        completion.done();
                                        return;
                                    }

                                    dbf.update(ref);

                                    if (resultMap.get(ref.getBackupStorageUuid()).compareAndSet(false, true)) {
                                        // In case 'Platform' etc. is changed.
                                        ImageVO vo = dbf.reload(ivo);
                                        vo.setMd5Sum(re.getMd5sum());
                                        vo.setSize(re.getSize());
                                        vo.setActualSize(re.getActualSize());
                                        vo.setUrl(URLBuilder.hideUrlPassword(vo.getUrl()));
                                        if (StringUtils.isNotEmpty(re.getFormat())) {
                                            vo.setFormat(re.getFormat());
                                        }
                                        if (vo.getFormat().equals(ImageConstant.ISO_FORMAT_STRING)
                                                && ImageMediaType.RootVolumeTemplate.equals(vo.getMediaType())) {
                                            vo.setMediaType(ImageMediaType.ISO);
                                        }
                                        if (ImageConstant.QCOW2_FORMAT_STRING.equals(vo.getFormat())
                                                && ImageMediaType.ISO.equals(vo.getMediaType())) {
                                            vo.setMediaType(ImageMediaType.RootVolumeTemplate);
                                        }

                                        if (resultMap.entrySet().stream().allMatch(entry -> entry.getValue().get())) {
                                            vo.setStatus(ref.getStatus());
                                        }

                                        dbf.update(vo);
                                    }

                                    if (isUpload(msgData.getUrl())) {
                                        logger.debug(String.format("created upload request, image[uuid:%s, name:%s] to backup storage[uuid:%s]",
                                                inv.getUuid(), inv.getName(), dmsg.getBackupStorageUuid()));
                                    } else {
                                        logger.debug(String.format("successfully downloaded image[uuid:%s, name:%s] to backup storage[uuid:%s]",
                                                inv.getUuid(), inv.getName(), dmsg.getBackupStorageUuid()));
                                    }
                                    pluginRgty.getExtensionList(AfterAddImageExtensionPoint.class).forEach(exp -> exp.saveEncryptAfterAddImage(vo.getUuid()));

                                }

                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                // check if the database still has the record of the image
                // if there is no record, that means user delete the image during the downloading,
                // then we need to cleanup
                ImageVO vo = dbf.reload(ivo);
                InnerEvent event = new InnerEvent();
                if (vo == null) {
                    event.error = (operr("image [uuid:%s] has been deleted", ivo.getUuid()));
                    SQL.New("delete from ImageBackupStorageRefVO where imageUuid = :uuid")
                            .param("uuid", ivo.getUuid())
                            .execute();
                    event.reply(evt);
                    return;
                }

                if (resultMap.entrySet().stream().allMatch(entry -> entry.getValue().get())) {
                    final ImageInventory einv = ImageInventory.valueOf(vo);

                    if (vo.getStatus() == ImageStatus.Ready) {
                        extEmitter.afterAddImage(einv);
                    }

                    event.inv = einv;
                } else {
                    final ErrorCode err;
                    if (errors.isEmpty()) {
                        err = err(SysErrors.CREATE_RESOURCE_ERROR, "Failed to download image[name:%s] on all backup storage%s.",
                                inv.getName(), msgData.getBackupStorageUuids());
                    } else {
                        err = err(SysErrors.CREATE_RESOURCE_ERROR, errors.get(0), "Failed to download image[name:%s] on all backup storage%s.",
                                inv.getName(), msgData.getBackupStorageUuids());
                    }

                    extEmitter.failedToAddImage(inv, err);

                    dbf.remove(ivo);
                    event.error = err;
                }

                tracker.runTrackTask();
                event.reply(evt);
            }
        }.start();
    }

    private void createRootVolumeTemplateFromRootVolume(CreateRootVolumeTemplateMessage msgData, String rootVolumeUuid, ReturnValueCompletion<ImageInventory> completion){
        ImageMessageFiller.fillFromVolume(msgData, rootVolumeUuid);

        final TaskProgressRange parentStage = getTaskStage();
        reportProgress(parentStage.getStart().toString());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-root-volume-%s", rootVolumeUuid));
        chain.preCheck(data -> buildErrIfCanceled());
        chain.then(new ShareFlow() {
            ImageVO imageVO;
            VolumeInventory rootVolume;
            Long imageEstimateSize;
            List<BackupStorageInventory> targetBackupStorages = new ArrayList<>();
            String zoneUuid;

            {
                VolumeVO rootvo = dbf.findByUuid(rootVolumeUuid, VolumeVO.class);
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
                        EstimateVolumeTemplateSizeMsg msg = new EstimateVolumeTemplateSizeMsg();
                        msg.setVolumeUuid(rootVolume.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, rootVolume.getPrimaryStorageUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                EstimateVolumeTemplateSizeReply sr = reply.castReply();
                                imageEstimateSize = sr.getActualSize();
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
                        q.add(VolumeVO_.uuid, Op.EQ, rootVolumeUuid);
                        final VolumeVO volvo = q.find();

                        imageVO = createImageInDb(msgData, imgvo -> {
                            imgvo.setFormat(volvo.getFormat());
                            imgvo.setUrl(String.format("volume://%s", rootVolumeUuid));
                            imgvo.setSize(volvo.getSize());
                            imgvo.setActualSize(imageEstimateSize);
                            imgvo.setArchitecture(dbf.findByUuid(rootVolume.getVmInstanceUuid(), VmInstanceVO.class).getArchitecture());
                        });

                        createSysTag(msgData, imageVO);

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (imageVO != null) {
                            dbf.remove(imageVO);
                            dbf.eoCleanup(ImageVO.class, imageVO.getUuid());
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "select-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (msgData.getBackupStorageUuids() == null) {
                            AllocateBackupStorageMsg abmsg = new AllocateBackupStorageMsg();
                            abmsg.setRequiredZoneUuid(zoneUuid);
                            abmsg.setSize(imageEstimateSize);
                            bus.makeLocalServiceId(abmsg, BackupStorageConstant.SERVICE_ID);

                            bus.send(abmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        targetBackupStorages.add(((AllocateBackupStorageReply) reply).getInventory());
                                        saveRefVOByBsInventorys(targetBackupStorages, imageVO.getUuid());
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        } else {
                            List<AllocateBackupStorageMsg> amsgs = CollectionUtils.transformToList(msgData.getBackupStorageUuids(), new Function<AllocateBackupStorageMsg, String>() {
                                @Override
                                public AllocateBackupStorageMsg call(String arg) {
                                    AllocateBackupStorageMsg abmsg = new AllocateBackupStorageMsg();
                                    abmsg.setSize(imageEstimateSize);
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
                                        trigger.fail(operr("unable to allocate backup storage specified by uuids%s, list errors are: %s",
                                                msgData.getBackupStorageUuids(), JSONObjectUtil.toJsonString(errs)));
                                    } else {
                                        saveRefVOByBsInventorys(targetBackupStorages, imageVO.getUuid());
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
                                rmsg.setSize(imageEstimateSize);
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
                                                imageEstimateSize, bs.getUuid(), r.getError()));
                                    }
                                }

                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "before-create-template-on-bs";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String volumePsUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, rootVolumeUuid)
                                .select(VolumeVO_.primaryStorageUuid)
                                .findValue();
                        for (BackupStorageInventory bs: targetBackupStorages) {
                            extEmitter.beforeCreateImage(ImageInventory.valueOf(imageVO),  bs.getUuid(), volumePsUuid);
                        }
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-creating-template";

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
                                    ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                                            .eq(ImageBackupStorageRefVO_.backupStorageUuid, bs.getUuid())
                                            .eq(ImageBackupStorageRefVO_.imageUuid, imageVO.getUuid())
                                            .find();

                                    if (dbf.reload(imageVO) == null) {
                                        SQL.New("delete from ImageBackupStorageRefVO where imageUuid = :uuid")
                                                .param("uuid", imageVO.getUuid())
                                                .execute();
                                        trigger.fail(operr("image [uuid:%s] has been deleted", imageVO.getUuid()));
                                        return;
                                    }


                                    if (!r.isSuccess()) {
                                        logger.warn(String.format("failed to create image from root volume[uuid:%s] on backup storage[uuid:%s], because %s",
                                                rootVolumeUuid, bs.getUuid(), r.getError()));
                                        err = r.getError();
                                        dbf.remove(ref);
                                        continue;
                                    }

                                    CreateTemplateFromVmRootVolumeReply reply = (CreateTemplateFromVmRootVolumeReply) r;
                                    ref.setStatus(ImageStatus.Ready);
                                    ref.setInstallPath(reply.getInstallPath());
                                    dbf.update(ref);

                                    imageVO.setStatus(ImageStatus.Ready);
                                    if (reply.getFormat() != null) {
                                        imageVO.setFormat(reply.getFormat());
                                    }
                                    imageVO = dbf.updateAndRefresh(imageVO);
                                    success = true;
                                    logger.debug(String.format("successfully created image[uuid:%s] from root volume[uuid:%s] on backup storage[uuid:%s]",
                                            imageVO.getUuid(), rootVolumeUuid, bs.getUuid()));
                                }

                                if (success) {
                                    trigger.next();
                                } else {
                                    trigger.fail(operr("failed to create image from root volume[uuid:%s] on all backup storage, see cause for one of errors",
                                            rootVolumeUuid).causedBy(err));
                                }
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "copy-system-tag-to-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SyncSystemTagFromVolumeMsg smsg = new SyncSystemTagFromVolumeMsg();
                        smsg.setImageUuid(imageVO.getUuid());
                        smsg.setVolumeUuid(rootVolumeUuid);
                        bus.makeLocalServiceId(smsg, ImageConstant.SERVICE_ID);
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("sync image[uuid:%s]system tag fail", rootVolumeUuid));
                                }
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-image-size";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        new While<>(targetBackupStorages).all((arg, compl) -> {
                            SyncImageSizeMsg smsg = new SyncImageSizeMsg();
                            smsg.setBackupStorageUuid(arg.getUuid());
                            smsg.setImageUuid(imageVO.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(smsg, ImageConstant.SERVICE_ID, imageVO.getUuid());
                            bus.send(smsg, new CloudBusCallBack(compl) {
                                @Override
                                public void run(MessageReply reply) {
                                    compl.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler((Message) msgData) {
                    @Override
                    public void handle(Map data) {
                        imageVO = dbf.reload(imageVO);
                        ImageInventory iinv = ImageInventory.valueOf(imageVO);

                        extEmitter.afterCreateImage(iinv);

                        logger.debug(String.format("successfully create template[uuid:%s] from root volume[uuid:%s]", iinv.getUuid(), rootVolumeUuid));
                        reportProgress(parentStage.getEnd().toString());
                        completion.success(iinv);
                    }
                });

                error(new FlowErrorHandler((Message) msgData) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to create template from root volume[uuid:%s], because %s", rootVolumeUuid, errCode));
                        completion.fail(errCode);
                    }
                });
            }
        }).start();

    }

    private void createDataVolumeTemplateFromVolume(CreateDataVolumeTemplateMessage msgData, String volumeUuid, ReturnValueCompletion<ImageInventory> completion){
        ImageMessageFiller.fillFromVolume(msgData, volumeUuid);

        final TaskProgressRange parentStage = getTaskStage();
        reportProgress(parentStage.getStart().toString());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-template-from-volume-%s", volumeUuid));
        chain.preCheck(data -> buildErrIfCanceled());
        chain.then(new ShareFlow() {
            List<BackupStorageInventory> backupStorages = new ArrayList<>();
            ImageVO image;
            long imageEstimateSize;
            String volumePsUuid;
            long imageActualSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-actual-size-of-data-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        EstimateVolumeTemplateSizeMsg smsg = new EstimateVolumeTemplateSizeMsg();
                        smsg.setVolumeUuid(volumeUuid);
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, volumeUuid);
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                EstimateVolumeTemplateSizeReply sr = reply.castReply();
                                imageEstimateSize = sr.getActualSize();
                                volumePsUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid)
                                        .select(VolumeVO_.primaryStorageUuid)
                                        .findValue();
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
                        q.add(VolumeVO_.uuid, Op.EQ, volumeUuid);
                        Tuple t = q.findTuple();

                        String format = t.get(0, String.class);
                        long size = t.get(1, Long.class);

                        image = createImageInDb(msgData, imgvo -> {
                            imgvo.setFormat(format);
                            imgvo.setUrl(String.format("volume://%s", volumeUuid));
                            imgvo.setSize(size);
                            imgvo.setActualSize(imageEstimateSize);
                        });

                        createSysTag(msgData, image);

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
                                q.setParameter("volUuid", volumeUuid);
                                return q.getSingleResult();
                            }
                        }.call();

                        if (msgData.getBackupStorageUuids() == null) {
                            AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                            amsg.setRequiredZoneUuid(zoneUuid);
                            amsg.setRequiredPrimaryStorageUuid(volumePsUuid);
                            amsg.setSize(imageEstimateSize);
                            bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        backupStorages.add(((AllocateBackupStorageReply) reply).getInventory());
                                        saveRefVOByBsInventorys(backupStorages, image.getUuid());
                                        trigger.next();
                                    } else {
                                        trigger.fail(operr(reply.getError(), "cannot find proper backup storage"));
                                    }
                                }
                            });
                        } else {
                            List<AllocateBackupStorageMsg> amsgs = CollectionUtils.transformToList(msgData.getBackupStorageUuids(), new Function<AllocateBackupStorageMsg, String>() {
                                @Override
                                public AllocateBackupStorageMsg call(String arg) {
                                    AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                                    amsg.setRequiredZoneUuid(zoneUuid);
                                    amsg.setSize(imageEstimateSize);
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
                                            backupStorages.add(((AllocateBackupStorageReply) r).getInventory());
                                        } else {
                                            errs.add(r.getError());
                                        }
                                    }

                                    if (backupStorages.isEmpty()) {
                                        trigger.fail(operr("failed to allocate all backup storage[uuid:%s], a list of error: %s",
                                                msgData.getBackupStorageUuids(), JSONObjectUtil.toJsonString(errs)));
                                    } else {
                                        saveRefVOByBsInventorys(backupStorages, image.getUuid());
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!backupStorages.isEmpty()) {
                            List<ReturnBackupStorageMsg> rmsgs = CollectionUtils.transformToList(backupStorages, new Function<ReturnBackupStorageMsg, BackupStorageInventory>() {
                                @Override
                                public ReturnBackupStorageMsg call(BackupStorageInventory arg) {
                                    ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                    rmsg.setBackupStorageUuid(arg.getUuid());
                                    rmsg.setSize(imageEstimateSize);
                                    bus.makeLocalServiceId(rmsg, BackupStorageConstant.SERVICE_ID);
                                    return rmsg;
                                }
                            });

                            bus.send(rmsgs, new CloudBusListCallBack(null) {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply r : replies) {
                                        BackupStorageInventory bs = backupStorages.get(replies.indexOf(r));
                                        logger.warn(String.format("failed to return %s bytes to backup storage[uuid:%s]", acntMgr, bs.getUuid()));
                                    }
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "before-create-template-on-bs";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (BackupStorageInventory bs: backupStorages) {
                            extEmitter.beforeCreateImage(ImageInventory.valueOf(image), bs.getUuid(), volumePsUuid);
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-data-volume-template-from-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        // FIXME: should create once and then upload different bs.
                        List<CreateDataVolumeTemplateFromDataVolumeMsg> cmsgs = CollectionUtils.transformToList(backupStorages, new Function<CreateDataVolumeTemplateFromDataVolumeMsg, BackupStorageInventory>() {
                            @Override
                            public CreateDataVolumeTemplateFromDataVolumeMsg call(BackupStorageInventory bs) {
                                CreateDataVolumeTemplateFromDataVolumeMsg cmsg = new CreateDataVolumeTemplateFromDataVolumeMsg();
                                cmsg.setVolumeUuid(volumeUuid);
                                cmsg.setBackupStorageUuid(bs.getUuid());
                                cmsg.setImageUuid(image.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, volumeUuid);
                                return cmsg;
                            }
                        });

                        bus.send(cmsgs, new CloudBusListCallBack(null) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                int fail = 0;
                                String mdsum = null;
                                ErrorCode err = null;
                                String format = null;
                                for (MessageReply r : replies) {
                                    BackupStorageInventory bs = backupStorages.get(replies.indexOf(r));
                                    if (!r.isSuccess()) {
                                        logger.warn(String.format("failed to create data volume template from volume[uuid:%s] on backup storage[uuid:%s], %s",
                                                volumeUuid, bs.getUuid(), r.getError()));
                                        fail++;
                                        err = r.getError();
                                        continue;
                                    }

                                    CreateDataVolumeTemplateFromDataVolumeReply reply = r.castReply();
                                    ImageBackupStorageRefVO vo = Q.New(ImageBackupStorageRefVO.class)
                                            .eq(ImageBackupStorageRefVO_.backupStorageUuid, bs.getUuid())
                                            .eq(ImageBackupStorageRefVO_.imageUuid, image.getUuid())
                                            .find();
                                    vo.setStatus(ImageStatus.Ready);
                                    vo.setInstallPath(reply.getInstallPath());
                                    dbf.update(vo);

                                    if (mdsum == null) {
                                        mdsum = reply.getMd5sum();
                                    }
                                    if (reply.getFormat() != null) {
                                        format = reply.getFormat();
                                    }

                                    imageActualSize = reply.getActualSize();
                                    if (reply.getActualSize() == 0) {
                                        imageActualSize = imageEstimateSize;
                                    }
                                }

                                int backupStorageNum = msgData.getBackupStorageUuids() == null ? 1 : msgData.getBackupStorageUuids().size();

                                if (fail == backupStorageNum) {
                                    ErrorCode errCode = operr("failed to create data volume template from volume[uuid:%s] on all backup storage%s. See cause for one of errors",
                                            volumeUuid, msgData.getBackupStorageUuids()).causedBy(err);

                                    trigger.fail(errCode);
                                } else {
                                    image = dbf.reload(image);
                                    if (format != null) {
                                        image.setFormat(format);
                                    }
                                    image.setMd5Sum(mdsum);
                                    image.setStatus(ImageStatus.Ready);
                                    image.setActualSize(imageActualSize);
                                    image = dbf.updateAndRefresh(image);

                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        reportProgress(parentStage.getEnd().toString());

                        ImageInventory inv = ImageInventory.valueOf(image);
                        extEmitter.afterCreateImage(inv);
                        completion.success(inv);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(CancelCreateRootVolumeTemplateFromRootVolumeMsg msg) {
        CancelCreateRootVolumeTemplateFromRootVolumeReply reply = new CancelCreateRootVolumeTemplateFromRootVolumeReply();
        cancelCreateTemplateFromVolume(msg, msg.getRootVolumeUuid(), msg.getImageUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CancelCreateDataVolumeTemplateFromVolumeMsg msg) {
        CancelCreateDataVolumeTemplateFromVolumeReply reply = new CancelCreateDataVolumeTemplateFromVolumeReply();
        cancelCreateTemplateFromVolume(msg, msg.getVolumeUuid(), msg.getImageUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void cancelCreateTemplateFromVolume(CancelMessage msg, String volumeUuid, String imageUuid, Completion completion) {
        ImageVO image = dbf.findByUuid(imageUuid, ImageVO.class);
        if (image == null || image.getBackupStorageRefs() == null || image.getBackupStorageRefs().isEmpty()) {
            completion.fail(noncancelableErr(i18n("image[uuid:%s] is not on creating, please wait for it to cancel itself.", imageUuid)));
            return;
        }

        VolumeVO volVO = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if (volVO == null) {
            completion.fail(noncancelableErr(i18n("volume[uuid:%s] has been deleted. no need to cancel", volumeUuid)));
            return;
        }

        List<String> bsUuids = image.getBackupStorageRefs().stream()
                .map(ImageBackupStorageRefVO::getBackupStorageUuid)
                .collect(Collectors.toList());

        CancelJobOnPrimaryStorageMsg cmsg = new CancelJobOnPrimaryStorageMsg();
        cmsg.setCancellationApiId(msg.getCancellationApiId());
        cmsg.setPrimaryStorageUuid(volVO.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec) {
        // TODO: move to compute module.

        String architecture = spec.getArchitecture();
        if (architecture == null && spec.getImage() != null) {
            architecture = spec.getImage().getArchitecture();
        }

        if (architecture == null && spec.getVmInstance() != null) {
            architecture = spec.getVmInstance().getArchitecture();
        }

        if (architecture == null) {
            return candidates;
        }

        String finalArchitecture = architecture;
        return candidates.stream().filter(it -> it.getArchitecture().equals(finalArchitecture)).collect(Collectors.toList());
    }

    @Override
    public String filterErrorReason() {
        return null;
    }
}
