package org.zstack.image;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.notification.N;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.progress.TaskProgressRange;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
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
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.getTaskStage;
import static org.zstack.core.progress.ProgressReportService.reportProgress;
import static org.zstack.header.Constants.THREAD_CONTEXT_API;
import static org.zstack.header.Constants.THREAD_CONTEXT_TASK_NAME;
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

    private void handle(AddImageMsg msg) {
        AddImageReply evt = new AddImageReply();
        AddImageLongJobData data = new AddImageLongJobData(msg);
        BeanUtils.copyProperties(msg, data);
        handleAddImageMsg(data, evt);
    }


    private void handle(CreateRootVolumeTemplateFromRootVolumeMsg msg) {
        CreateRootVolumeTemplateFromRootVolumeData data = new CreateRootVolumeTemplateFromRootVolumeData(msg);
        BeanUtils.copyProperties(msg, data);
        handleCreateRootVolumeTemplateFromRootVolumeMsg(data, new CreateRootVolumeTemplateFromRootVolumeReply());
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AddImageMsg) {
            handle((AddImageMsg) msg);
        } else if (msg instanceof CreateRootVolumeTemplateFromRootVolumeMsg){
            handle((CreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof CreateDataVolumeTemplateFromVolumeMsg){
            handle ((CreateDataVolumeTemplateFromVolumeMsg)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CreateDataVolumeTemplateFromVolumeMsg msg) {
        CreateDataVolumeTemplateFromVolumeLongJobData data = new CreateDataVolumeTemplateFromVolumeLongJobData(msg);
        BeanUtils.copyProperties(msg, data);
        handleCreateDataVolumeTemplateFromVolumeMsg(data, new CreateDataVolumeTemplateFromVolumeReply());
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
        CreateDataVolumeTemplateFromVolumeLongJobData data = new CreateDataVolumeTemplateFromVolumeLongJobData(msg);
        BeanUtils.copyProperties(msg, data);
        handleCreateDataVolumeTemplateFromVolumeMsg(data, new APICreateDataVolumeTemplateFromVolumeEvent(msg.getId()));
    }

    private void handle(final APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        final APICreateRootVolumeTemplateFromVolumeSnapshotEvent evt = new APICreateRootVolumeTemplateFromVolumeSnapshotEvent(msg.getId());

        class Result {
            List<CreateTemplateFromVolumeSnapshotMsg> msgs;
            ImageVO image;
        }

        Result res = new Result();
        new SQLBatch() {
            @Override
            protected void scripts() {
                String format = q(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.format)
                        .eq(VolumeSnapshotVO_.uuid, msg.getSnapshotUuid()).findValue();

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
                vo.setGuestOsType(msg.getGuestOsType());
                vo.setStatus(ImageStatus.Creating);
                vo.setState(ImageState.Enabled);
                vo.setFormat(format);
                vo.setMediaType(ImageMediaType.RootVolumeTemplate);
                vo.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
                vo.setUrl(String.format("volumeSnapshot://%s", msg.getSnapshotUuid()));
                vo.setAccountUuid(msg.getSession().getAccountUuid());
                persist(vo);

                tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), ImageVO.class.getSimpleName());

                Tuple t = q(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid)
                        .eq(VolumeSnapshotVO_.uuid, msg.getSnapshotUuid()).findTuple();
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

                res.msgs = cmsgs;
                res.image = vo;
            }
        }.execute();

        List<CreateTemplateFromVolumeSnapshotMsg> cmsgs = res.msgs;
        ImageVO vo = res.image;

        List<Failure> failures = new ArrayList<>();
        AsyncLatch latch = new AsyncLatch(cmsgs.size(), new NoErrorCompletion(msg) {
            @Override
            public void done() {
                if (failures.size() == cmsgs.size()) {
                    // failed on all
                    ErrorCodeList error = errf.stringToOperationError(String.format("failed to create template from" +
                                    " the volume snapshot[uuid:%s] on backup storage[uuids:%s]", msg.getSnapshotUuid(),
                            msg.getBackupStorageUuids()), failures.stream().map(f -> f.error).collect(Collectors.toList()));
                    evt.setError(error);
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
        CreateRootVolumeTemplateFromRootVolumeData data = new CreateRootVolumeTemplateFromRootVolumeData(msg);
        BeanUtils.copyProperties(msg, data);
        handleCreateRootVolumeTemplateFromRootVolumeMsg(data, new APICreateRootVolumeTemplateFromRootVolumeEvent(msg.getId()));
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
                        bus.send(msg, new CloudBusCallBack(null) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    N.New(ImageVO.class, imageUuid).warn_("failed to expunge the image[uuid:%s] on the backup storage[uuid:%s], will try it later. %s",
                                            imageUuid, bsUuid, reply.getError());
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
            private void check(APIRecoverImageMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getImageUuid());

                long imageNumQuota = pairs.get(ImageQuotaConstant.IMAGE_NUM).getValue();
                long imageSizeQuota = pairs.get(ImageQuotaConstant.IMAGE_SIZE).getValue();
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

            @Transactional(readOnly = true)
            private void check(APIAddImageMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

                checkImageNumQuota(currentAccountUuid, resourceTargetOwnerAccountUuid, pairs);
                new ImageQuotaUtil().checkImageSizeQuotaUseHttpHead(msg, pairs);
            }

            private void check(APICreateRootVolumeTemplateFromRootVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkImageNumQuota(msg.getSession().getAccountUuid(),
                        msg.getSession().getAccountUuid(),
                        pairs);
            }

            private void check(APICreateDataVolumeTemplateFromVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkImageNumQuota(msg.getSession().getAccountUuid(),
                        msg.getSession().getAccountUuid(),
                        pairs);
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

    private void doReportProgress(String apiId, String taskName, long progress) {
        ThreadContext.put(THREAD_CONTEXT_API, apiId);
        ThreadContext.put(THREAD_CONTEXT_TASK_NAME, taskName);
        reportProgress(String.valueOf(progress));
    }

    private void trackUpload(String name, String imageUuid, String bsUuid, String hostname) {
        final int maxNumOfFailure = 3;
        final int maxIdleSecond = 30;

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            private long numError = 0;
            private int numTicks = 0;

            private void markCompletion(final GetImageDownloadProgressReply dr) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        ImageVO vo = findByUuid(imageUuid, ImageVO.class);
                        if (StringUtils.isNotEmpty(dr.getFormat())) {
                            vo.setFormat(dr.getFormat());
                        }
                        if (vo.getFormat().equals(ImageConstant.ISO_FORMAT_STRING)
                                && ImageMediaType.RootVolumeTemplate.equals(vo.getMediaType())) {
                            vo.setMediaType(ImageMediaType.ISO);
                        }
                        if (ImageConstant.QCOW2_FORMAT_STRING.equals(vo.getFormat())
                                && ImageMediaType.ISO.equals(vo.getMediaType())) {
                            vo.setMediaType(ImageMediaType.RootVolumeTemplate);
                        }
                        vo.setStatus(ImageStatus.Ready);
                        vo.setSize(dr.getSize());
                        vo.setActualSize(dr.getActualSize());
                        merge(vo);
                        sql(ImageBackupStorageRefVO.class)
                                .eq(ImageBackupStorageRefVO_.backupStorageUuid, bsUuid)
                                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                                .set(ImageBackupStorageRefVO_.status, ImageStatus.Ready)
                                .set(ImageBackupStorageRefVO_.installPath, dr.getInstallPath())
                                .update();
                    }
                }.execute();

                N.New(ImageVO.class, imageUuid).info_("added image [name: %s, uuid: %s]", name, imageUuid);
            }

            private void markFailure(ErrorCode reason) {
                N.New(ImageVO.class, imageUuid).error_("upload image [name: %s, uuid: %s] failed: %s",
                        name, imageUuid, reason.toString());

                // Note, the handler of ImageDeletionMsg will deal with storage capacity.
                ImageDeletionMsg msg = new ImageDeletionMsg();
                msg.setImageUuid(imageUuid);
                msg.setBackupStorageUuids(Collections.singletonList(bsUuid));
                msg.setDeletionPolicy(ImageDeletionPolicy.Direct.toString());
                msg.setForceDelete(true);
                bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imageUuid);
                bus.send(msg);
            }

            @Override
            public boolean run() {
                final ImageVO ivo = dbf.findByUuid(imageUuid, ImageVO.class);
                if (ivo == null) {
                    // If image VO not existed, stop tracking.
                    return true;
                }

                numTicks += 1;
                if (ivo.getActualSize() == 0 && numTicks * getInterval() >= maxIdleSecond) {
                    markFailure(operr("upload session expired"));
                    return true;
                }

                final GetImageDownloadProgressMsg dmsg = new GetImageDownloadProgressMsg();
                dmsg.setBackupStorageUuid(bsUuid);
                dmsg.setImageUuid(imageUuid);
                dmsg.setHostname(hostname);
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, bsUuid);

                final MessageReply reply = bus.call(dmsg);
                if (reply.isSuccess()) {
                    // reset the error counter
                    numError = 0;

                    final GetImageDownloadProgressReply dr = reply.castReply();

                    if (dr.isCompleted()) {
                        if (!dr.isSuccess()) {
                            markFailure(dr.getError());
                        } else {
                            doReportProgress(imageUuid, "adding to image store", 100);
                            markCompletion(dr);
                        }
                        return true;
                    }

                    doReportProgress(imageUuid, "uploading image", dr.getProgress());
                    if (ivo.getActualSize() == 0 && dr.getActualSize() != 0) {
                        ivo.setActualSize(dr.getActualSize());
                        dbf.updateAndRefresh(ivo);

                        AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                        amsg.setBackupStorageUuid(bsUuid);
                        amsg.setSize(dr.getActualSize());
                        bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                        MessageReply areply = bus.call(amsg);
                        if (!areply.isSuccess()) {
                            markFailure(areply.getError());
                            return true;
                        }
                    }

                    return false;
                }

                numError++;
                if (numError <= maxNumOfFailure) {
                    return false;
                }

                markFailure(reply.getError());
                return true;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 3;
            }

            @Override
            public String getName() {
                return String.format("tracking upload image [name: %s, uuid: %s]", name, imageUuid);
            }
        });
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
        vo.setPlatform(ImagePlatform.valueOf(msgData.getPlatform()));

        ImageFactory factory = getImageFacotry(ImageType.valueOf(imageType));
        final ImageVO ivo = new SQLBatchWithReturn<ImageVO>() {
            @Override
            protected ImageVO scripts() {
                vo.setAccountUuid(accountUuid);
                final ImageVO ivo = factory.createImage(vo);
                tagMgr.createTags(msgData.getSystemTags(), msgData.getUserTags(), vo.getUuid(), ImageVO.class.getSimpleName());
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
        for (AddImageExtensionPoint ext : pluginRgty.getExtensionList(AddImageExtensionPoint.class)) {
            ext.preAddImage(inv);
        }
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

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class), ext -> ext.beforeAddImage(inv));
        new LoopAsyncBatch<DownloadImageMsg>(msgData.getNeedReplyMessage()) {
            AtomicBoolean success = new AtomicBoolean(false);

            class TrackContext {
                String name;
                String imageUuid;
                String bsUuid;
                String hostname;
            }

            List<TrackContext> ctxs = new ArrayList<>();

            private void addTrackTask(String name, String imageUuid, String bsUuid, String installPath) throws URISyntaxException {
                TrackContext ctx = new TrackContext();
                ctx.name = name;
                ctx.imageUuid = imageUuid;
                ctx.bsUuid = bsUuid;
                ctx.hostname = new URI(installPath).getHost();
                ctxs.add(ctx);
            }

            private void runTrackTask() {
                for (TrackContext ctx : ctxs) {
                    trackUpload(ctx.name, ctx.imageUuid, ctx.bsUuid, ctx.hostname);
                }
            }

            @Override
            protected Collection<DownloadImageMsg> collect() {
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
                                    if (isUpload(msgData.getUrl())) {
                                        try {
                                            addTrackTask(ivo.getName(), ivo.getUuid(), ref.getBackupStorageUuid(), re.getInstallPath());
                                        } catch (URISyntaxException e) {
                                            throw new OperationFailureException(errf.throwableToOperationError(e));
                                        }
                                    } else {
                                        ref.setStatus(ImageStatus.Ready);
                                    }
                                    ref.setInstallPath(re.getInstallPath());

                                    if (dbf.reload(ref) == null) {
                                        logger.debug(String.format("image[uuid: %s] has been deleted", ref.getImageUuid()));
                                        completion.done();
                                        return;
                                    }

                                    dbf.update(ref);

                                    if (success.compareAndSet(false, true)) {
                                        // In case 'Platform' etc. is changed.
                                        ImageVO vo = dbf.reload(ivo);
                                        vo.setMd5Sum(re.getMd5sum());
                                        vo.setSize(re.getSize());
                                        vo.setActualSize(re.getActualSize());
                                        vo.setStatus(ref.getStatus());
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
                                        dbf.update(vo);
                                    }

                                    if (isUpload(msgData.getUrl())) {
                                        logger.debug(String.format("created upload request, image[uuid:%s, name:%s] to backup storage[uuid:%s]",
                                                inv.getUuid(), inv.getName(), dmsg.getBackupStorageUuid()));
                                    } else {
                                        logger.debug(String.format("successfully downloaded image[uuid:%s, name:%s] to backup storage[uuid:%s]",
                                                inv.getUuid(), inv.getName(), dmsg.getBackupStorageUuid()));
                                    }
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

                if (success.get()) {
                    final ImageInventory einv = ImageInventory.valueOf(vo);

                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class), new ForEachFunction<AddImageExtensionPoint>() {
                        @Override
                        public void run(AddImageExtensionPoint ext) {
                            ext.afterAddImage(einv);
                        }
                    });

                    event.inv = einv;
                } else {
                    final ErrorCode err = errf.instantiateErrorCode(SysErrors.CREATE_RESOURCE_ERROR, String.format("Failed to download image[name:%s] on all backup storage%s.",
                            inv.getName(), msgData.getBackupStorageUuids()), errors);

                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class), new ForEachFunction<AddImageExtensionPoint>() {
                        @Override
                        public void run(AddImageExtensionPoint ext) {
                            ext.failedToAddImage(inv, err);
                        }
                    });

                    dbf.remove(ivo);
                    event.error = err;
                }

                runTrackTask();
                event.reply(evt);
            }
        }.start();
    }

    private void handleCreateRootVolumeTemplateFromRootVolumeMsg(CreateRootVolumeTemplateFromRootVolumeData msgData, Message evt){
        class InnerEvent extends Message {
            ErrorCode error;
            ImageInventory inv;

            void reply(Message reply) {
                if (evt instanceof APICreateRootVolumeTemplateFromRootVolumeEvent) {
                    APICreateRootVolumeTemplateFromRootVolumeEvent event = (APICreateRootVolumeTemplateFromRootVolumeEvent) reply;
                    if (null != error) {
                        event.setError(error);
                    }
                    if (null != inv) {
                        event.setInventory(inv);
                    }
                    bus.publish(event);
                } else if (evt instanceof CreateRootVolumeTemplateFromRootVolumeReply) {
                    CreateRootVolumeTemplateFromRootVolumeReply reply1 = (CreateRootVolumeTemplateFromRootVolumeReply) reply;
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
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-root-volume-%s", msgData.getRootVolumeUuid()));
        chain.then(new ShareFlow() {
            ImageVO imageVO;
            VolumeInventory rootVolume;
            Long imageActualSize;
            List<BackupStorageInventory> targetBackupStorages = new ArrayList<>();
            String zoneUuid;

            {
                VolumeVO rootvo = dbf.findByUuid(msgData.getRootVolumeUuid(), VolumeVO.class);
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

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                        q.add(VolumeVO_.uuid, Op.EQ, msgData.getRootVolumeUuid());
                        final VolumeVO volvo = q.find();

                        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(volvo.getUuid());

                        final ImageVO imvo = new ImageVO();
                        if (msgData.getResourceUuid() != null) {
                            imvo.setUuid(msgData.getResourceUuid());
                        } else {
                            imvo.setUuid(Platform.getUuid());
                        }
                        imvo.setDescription(msgData.getDescription());
                        imvo.setMediaType(ImageMediaType.RootVolumeTemplate);
                        imvo.setState(ImageState.Enabled);
                        imvo.setGuestOsType(msgData.getGuestOsType());
                        imvo.setFormat(volvo.getFormat());
                        imvo.setName(msgData.getName());
                        imvo.setSystem(msgData.isSystem());
                        imvo.setPlatform(ImagePlatform.valueOf(msgData.getPlatform()));
                        imvo.setStatus(ImageStatus.Downloading);
                        imvo.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
                        imvo.setUrl(String.format("volume://%s", msgData.getRootVolumeUuid()));
                        imvo.setSize(volvo.getSize());
                        imvo.setActualSize(imageActualSize);
                        imvo.setAccountUuid(accountUuid);
                        dbf.persist(imvo);
                        tagMgr.createTags(msgData.getNeedReplyMessage().getSystemTags(),msgData.getNeedReplyMessage().getUserTags(), imvo.getUuid(), ImageVO.class.getSimpleName());

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
                        List<ImageBackupStorageRefVO> refs = new ArrayList<>();
                        if (msgData.getBackupStorageUuids() == null) {
                            AllocateBackupStorageMsg abmsg = new AllocateBackupStorageMsg();
                            abmsg.setRequiredZoneUuid(zoneUuid);
                            abmsg.setSize(imageActualSize);
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
                                                msgData.getRootVolumeUuid(), bs.getUuid(), r.getError()));
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
                                            imageVO.getUuid(), msgData.getRootVolumeUuid(), bs.getUuid()));
                                }

                                if (success) {
                                    trigger.next();
                                } else {
                                    trigger.fail(operr("failed to create image from root volume[uuid:%s] on all backup storage, see cause for one of errors",
                                            msgData.getRootVolumeUuid()).causedBy(err));
                                }
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "copy-system-tag-to-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        // find the rootimage and create some systemtag if it has
                        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                        q.add(VolumeVO_.uuid, SimpleQuery.Op.EQ, msgData.getRootVolumeUuid());
                        q.select(VolumeVO_.vmInstanceUuid);
                        String vmInstanceUuid = q.findValue();
                        if (tagMgr.hasSystemTag(vmInstanceUuid, ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat())) {
                            tagMgr.createNonInherentSystemTag(imageVO.getUuid(),
                                    ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat(),
                                    ImageVO.class.getSimpleName());
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-image-size");

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        new While<>(targetBackupStorages).all((arg, completion) -> {
                            SyncImageSizeMsg smsg = new SyncImageSizeMsg();
                            smsg.setBackupStorageUuid(arg.getUuid());
                            smsg.setImageUuid(imageVO.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(smsg, ImageConstant.SERVICE_ID, imageVO.getUuid());
                            bus.send(smsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    completion.done();
                                }
                            });
                        }).run(new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msgData.getNeedReplyMessage()) {
                    @Override
                    public void handle(Map data) {
                        InnerEvent innerEvent = new InnerEvent();
                        imageVO = dbf.reload(imageVO);
                        ImageInventory iinv = ImageInventory.valueOf(imageVO);
                        innerEvent.inv = iinv;
                        logger.warn(String.format("successfully create template[uuid:%s] from root volume[uuid:%s]", iinv.getUuid(), msgData.getRootVolumeUuid()));
                        innerEvent.reply(evt);
                    }
                });

                error(new FlowErrorHandler(msgData.getNeedReplyMessage()) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        InnerEvent innerEvent = new InnerEvent();
                        innerEvent.error = errCode;
                        logger.warn(String.format("failed to create template from root volume[uuid:%s], because %s", msgData.getRootVolumeUuid(), errCode));
                        innerEvent.reply(evt);
                    }
                });
            }
        }).start();

    }

    private void handleCreateDataVolumeTemplateFromVolumeMsg(CreateDataVolumeTemplateFromVolumeLongJobData msgData, Message evt){
        class InnerEvent extends Message {
            ErrorCode error;
            ImageInventory inv;

            void reply(Message reply) {
                if (evt instanceof APICreateDataVolumeTemplateFromVolumeEvent) {
                    APICreateDataVolumeTemplateFromVolumeEvent event = (APICreateDataVolumeTemplateFromVolumeEvent) reply;
                    if (null != error) {
                        event.setError(error);
                    }
                    if (null != inv) {
                        event.setInventory(inv);
                    }
                    bus.publish(event);
                } else if (evt instanceof CreateDataVolumeTemplateFromVolumeReply) {
                    CreateDataVolumeTemplateFromVolumeReply reply1 = (CreateDataVolumeTemplateFromVolumeReply) reply;
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

        final TaskProgressRange parentStage = getTaskStage();

        List<ImageBackupStorageRefVO> refs = new ArrayList<>();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-template-from-volume-%s", msgData.getVolumeUuid()));
        chain.then(new ShareFlow() {
            List<BackupStorageInventory> backupStorages = new ArrayList<>();
            ImageVO image;
            long actualSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-actual-size-of-data-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
                        smsg.setVolumeUuid(msgData.getVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, msgData.getVolumeUuid());
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
                        q.add(VolumeVO_.uuid, Op.EQ, msgData.getVolumeUuid());
                        Tuple t = q.findTuple();

                        String format = t.get(0, String.class);
                        long size = t.get(1, Long.class);

                        final ImageVO vo = new ImageVO();
                        vo.setUuid(msgData.getResourceUuid() == null ? Platform.getUuid() : msgData.getResourceUuid());
                        vo.setName(msgData.getName());
                        vo.setDescription(msgData.getDescription());
                        vo.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
                        vo.setMediaType(ImageMediaType.DataVolumeTemplate);
                        vo.setSize(size);
                        vo.setActualSize(actualSize);
                        vo.setState(ImageState.Enabled);
                        vo.setStatus(ImageStatus.Creating);
                        vo.setSystem(false);
                        vo.setFormat(format);
                        vo.setUrl(String.format("volume://%s", msgData.getVolumeUuid()));
                        vo.setAccountUuid(msgData.getSession().getAccountUuid());
                        image = dbf.persistAndRefresh(vo);
                        tagMgr.createTags(msgData.getSystemTags(), msgData.getUserTags(), vo.getUuid(), ImageVO.class.getSimpleName());
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
                                q.setParameter("volUuid", msgData.getVolumeUuid());
                                return q.getSingleResult();
                            }
                        }.call();

                        if (msgData.getBackupStorageUuids() == null) {
                            AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                            amsg.setRequiredZoneUuid(zoneUuid);
                            amsg.setSize(actualSize);
                            bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        backupStorages.add(((AllocateBackupStorageReply) reply).getInventory());
                                        saveRefVOByBsInventorys(backupStorages, image.getUuid());

                                        for (BackupStorageInventory bs: backupStorages) {
                                            for (CreateImageExtensionPoint ext : pluginRgty.getExtensionList(CreateImageExtensionPoint.class)) {
                                                VolumeVO volume = dbf.findByUuid(msgData.getVolumeUuid(), VolumeVO.class);
                                                ext.beforeCreateImage(ImageInventory.valueOf(image), bs.getUuid(), volume.getPrimaryStorageUuid());
                                            }
                                        }
                                        trigger.next();
                                    } else {
                                        trigger.fail(errf.stringToOperationError("cannot find proper backup storage", reply.getError()));
                                    }
                                }
                            });
                        } else {
                            List<AllocateBackupStorageMsg> amsgs = CollectionUtils.transformToList(msgData.getBackupStorageUuids(), new Function<AllocateBackupStorageMsg, String>() {
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
                                        for (BackupStorageInventory bs: backupStorages) {
                                            for (CreateImageExtensionPoint ext : pluginRgty.getExtensionList(CreateImageExtensionPoint.class)) {
                                                VolumeVO volume = dbf.findByUuid(msgData.getVolumeUuid(), VolumeVO.class);

                                                ext.beforeCreateImage(ImageInventory.valueOf(image), bs.getUuid(), volume.getPrimaryStorageUuid());
                                            }
                                        }
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
                                    rmsg.setSize(actualSize);
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
                    String __name__ = "create-data-volume-template-from-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CreateDataVolumeTemplateFromDataVolumeMsg> cmsgs = CollectionUtils.transformToList(backupStorages, new Function<CreateDataVolumeTemplateFromDataVolumeMsg, BackupStorageInventory>() {
                            @Override
                            public CreateDataVolumeTemplateFromDataVolumeMsg call(BackupStorageInventory bs) {
                                CreateDataVolumeTemplateFromDataVolumeMsg cmsg = new CreateDataVolumeTemplateFromDataVolumeMsg();
                                cmsg.setVolumeUuid(msgData.getVolumeUuid());
                                cmsg.setBackupStorageUuid(bs.getUuid());
                                cmsg.setImageUuid(image.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, msgData.getVolumeUuid());
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
                                                msgData.getVolumeUuid(), bs.getUuid(), r.getError()));
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
                                }

                                int backupStorageNum = msgData.getBackupStorageUuids() == null ? 1 : msgData.getBackupStorageUuids().size();

                                if (fail == backupStorageNum) {
                                    ErrorCode errCode = operr("failed to create data volume template from volume[uuid:%s] on all backup storage%s. See cause for one of errors",
                                            msgData.getVolumeUuid(), msgData.getBackupStorageUuids()).causedBy(err);

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

                done(new FlowDoneHandler(msgData.getNeedReplyMessage()) {
                    @Override
                    public void handle(Map data) {
                        reportProgress(parentStage.getEnd().toString());
                        InnerEvent innerEvent = new InnerEvent();
                        innerEvent.inv = ImageInventory.valueOf(image);
                        innerEvent.reply(evt);
                    }
                });

                error(new FlowErrorHandler(msgData.getNeedReplyMessage()) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        InnerEvent innerEvent = new InnerEvent();
                        innerEvent.error = errCode;
                        innerEvent.reply(evt);
                    }
                });
            }
        }).start();
    }
}
