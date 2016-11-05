package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.gc.*;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 5:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ImageBase implements Image {
    private static final CLogger logger = Utils.getLogger(ImageBase.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ImageDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    private GCFacade gcf;
    @Autowired
    private PluginRegistry pluginRgty;

    protected ImageVO self;

    public ImageBase(ImageVO vo) {
        self = vo;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    protected ImageVO getSelf() {
        return self;
    }

    protected ImageInventory getSelfInventory() {
        return ImageInventory.valueOf(getSelf());
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ImageDeletionMsg) {
            handle((ImageDeletionMsg) msg);
        } else if (msg instanceof ExpungeImageMsg) {
            handle((ExpungeImageMsg) msg);
        } else if (msg instanceof SyncImageSizeMsg) {
            handle((SyncImageSizeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    class ImageSize {
        long size;
        long actualSize;
    }

    private void handle(final SyncImageSizeMsg msg) {
        final SyncImageSizeReply reply = new SyncImageSizeReply();

        syncImageSize(msg.getBackupStorageUuid(), new ReturnValueCompletion<ImageSize>(msg) {
            @Override
            public void success(ImageSize ret) {
                reply.setActualSize(ret.actualSize);
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void syncImageSize(String backupStorageUuid, final ReturnValueCompletion<ImageSize> completion) {
        if (backupStorageUuid == null) {
            List<String> bsUuids = CollectionUtils.transformToList(self.getBackupStorageRefs(), new Function<String, ImageBackupStorageRefVO>() {
                @Override
                public String call(ImageBackupStorageRefVO arg) {
                    return arg.getBackupStorageUuid();
                }
            });

            if (bsUuids.isEmpty()) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the image[uuid:%s, name:%s] is not on any backup storage", self.getUuid(), self.getName())
                ));
            }

            SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
            q.select(BackupStorageVO_.uuid);
            q.add(BackupStorageVO_.uuid, Op.IN, bsUuids);
            q.add(BackupStorageVO_.status, Op.EQ, BackupStorageStatus.Connected);
            q.setLimit(1);
            backupStorageUuid = q.findValue();
            if (backupStorageUuid == null) {
                completion.fail(errf.stringToOperationError(
                        String.format("No connected backup storage found for image[uuid:%s, name:%s]",
                                self.getUuid(), self.getName())));
                return;
            }
        }

        SyncImageSizeOnBackupStorageMsg smsg = new SyncImageSizeOnBackupStorageMsg();
        smsg.setBackupStorageUuid(backupStorageUuid);
        smsg.setImage(ImageInventory.valueOf(self));
        bus.makeTargetServiceIdByResourceUuid(smsg, BackupStorageConstant.SERVICE_ID, backupStorageUuid);
        bus.send(smsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    SyncImageSizeOnBackupStorageReply sr = reply.castReply();
                    self.setSize(sr.getSize());
                    self.setActualSize(sr.getActualSize());
                    dbf.update(self);

                    ImageSize ret = new ImageSize();
                    ret.actualSize = sr.getActualSize();
                    ret.size = sr.getSize();
                    completion.success(ret);
                }
            }
        });
    }


    private void handle(final ExpungeImageMsg msg) {
        final ExpungeImageReply reply = new ExpungeImageReply();
        final ImageBackupStorageRefVO ref = CollectionUtils.find(
                self.getBackupStorageRefs(),
                new Function<ImageBackupStorageRefVO, ImageBackupStorageRefVO>() {
                    @Override
                    public ImageBackupStorageRefVO call(ImageBackupStorageRefVO arg) {
                        return arg.getBackupStorageUuid().equals(msg.getBackupStorageUuid()) ? arg : null;
                    }
                }
        );

        if (ref == null) {
            logger.debug(String.format("cannot find reference for the image[uuid:%s] on the backup storage[uuid:%s], assume it's been deleted",
                    self.getUuid(), msg.getBackupStorageUuid()));
            bus.reply(msg, reply);
            return;
        }

        DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
        dmsg.setBackupStorageUuid(ref.getBackupStorageUuid());
        dmsg.setInstallPath(ref.getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dmsg.getBackupStorageUuid());
        bus.send(dmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    //TODO
                    logger.warn(String.format("failed to delete image[uuid:%s, name:%s] from backup storage[uuid:%s] because %s, need to garbage collect it",
                            self.getUuid(), self.getName(), r.getError(), ref.getBackupStorageUuid()));
                    reply.setError(r.getError());
                } else {
                    returnBackupStorageCapacity(ref.getBackupStorageUuid(), self.getActualSize());
                    //TODO remove ref from metadata, this logic should after all refs deleted
                    logger.debug("meilei. delete metadata");
                    logger.debug(String.format("meilei. imageInventory %s", ImageInventory.valueOf(self).toString()));
                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(ExpungeImageExtensionPoint.class), new ForEachFunction<ExpungeImageExtensionPoint>() {
                        @Override
                        public void run(ExpungeImageExtensionPoint ext) {
                            ext.afterExpungeImage(ImageInventory.valueOf(self), ref.getBackupStorageUuid());
                        }
                    });
                    dbf.remove(ref);
                    logger.debug(String.format("successfully expunged the image[uuid: %s, name: %s] on the backup storage[uuid: %s]",
                            self.getUuid(), self.getName(), ref.getBackupStorageUuid()));
                    self = dbf.reload(self);
                    if (self.getBackupStorageRefs().isEmpty()) {
                        logger.debug(String.format("the image[uuid:%s, name:%s] has been expunged on all backup storage, remove it from database",
                                self.getUuid(), self.getName()));
                        dbf.remove(self);
                    }
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void returnBackupStorageCapacity(final String bsUuid, final long size) {
        ReturnBackupStorageMsg msg = new ReturnBackupStorageMsg();
        msg.setBackupStorageUuid(bsUuid);
        msg.setSize(size);
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, bsUuid);
        bus.send(msg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    //TODO
                    logger.warn(String.format("failed to return capacity[%s] to the backup storage[uuid:%s]", size, bsUuid));
                }
            }
        });
    }

    private void handle(final ImageDeletionMsg msg) {
        final ImageDeletionReply reply = new ImageDeletionReply();
        if (self.getBackupStorageRefs().isEmpty()) {
            // the image is not on any backup storage; mostly likely the image is not in the status of Ready, for example
            // it's still downloading
            // in this case, we directly delete it from the database
            dbf.remove(self);
            bus.reply(msg, reply);
            return;
        }

        final ImageDeletionPolicy deletionPolicy = msg.getDeletionPolicy() == null
                ? deletionPolicyMgr.getDeletionPolicy(self.getUuid())
                : ImageDeletionPolicy.valueOf(msg.getDeletionPolicy());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-image-%s", self.getUuid()));
        Collection<ImageBackupStorageRefVO> toDelete = msg.getBackupStorageUuids() == null
                ? self.getBackupStorageRefs()
                : CollectionUtils.transformToList(
                self.getBackupStorageRefs(),
                new Function<ImageBackupStorageRefVO, ImageBackupStorageRefVO>() {
                    @Override
                    public ImageBackupStorageRefVO call(ImageBackupStorageRefVO arg) {
                        return msg.getBackupStorageUuids().contains(arg.getBackupStorageUuid()) ? arg : null;
                    }
                }
        );

        for (final ImageBackupStorageRefVO ref : toDelete) {
            chain.then(new NoRollbackFlow() {
                String __name__ = String.format("delete-image-%s-from-backup-storage-%s", self.getUuid(), ref.getBackupStorageUuid());

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    if (deletionPolicy == ImageDeletionPolicy.Direct) {
                        DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
                        dmsg.setBackupStorageUuid(ref.getBackupStorageUuid());
                        dmsg.setInstallPath(ref.getInstallPath());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dmsg.getBackupStorageUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    //TODO
                                    logger.warn(String.format("failed to delete image[uuid:%s, name:%s] from backup storage[uuid:%s] because %s," +
                                                    " need to garbage collect it",
                                            self.getUuid(), self.getName(), reply.getError(), ref.getBackupStorageUuid()));
                                } else {
                                    returnBackupStorageCapacity(ref.getBackupStorageUuid(), self.getActualSize());
                                    dbf.remove(ref);
                                    //TODO should delete ref in metadata
                                }
                                trigger.next();
                            }
                        });
                    } else if (deletionPolicy == ImageDeletionPolicy.DeleteReference) {
                        dbf.remove(ref);
                        logger.debug(String.format("delete the image[uuid: %s, name:%s]'s reference of the backup storage[uuid:%s]",
                                self.getUuid(), self.getName(), ref.getBackupStorageUuid()));
                        trigger.next();
                    } else {
                        ref.setStatus(ImageStatus.Deleted);
                        dbf.update(ref);
                        trigger.next();
                    }
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                self = dbf.reload(self);
                if (self.getBackupStorageRefs().isEmpty()) {
                    dbf.remove(self);
                    if (deletionPolicy == ImageDeletionPolicy.DeleteReference) {
                        logger.debug(String.format("successfully directly deleted the image[uuid:%s, name:%s] from the database," +
                                " as the policy is DeleteReference, it's still on the physical backup storage", self.getUuid(), self.getName()));
                    } else {
                        logger.debug(String.format("successfully directly deleted the image[uuid:%s, name:%s]", self.getUuid(), self.getName()));
                    }
                } else {
                    int deleteCount = 0;
                    for (ImageBackupStorageRefVO ref : self.getBackupStorageRefs()) {
                        if (ref.getStatus() == ImageStatus.Deleted) {
                            deleteCount++;
                        }
                    }
                    if (deleteCount == self.getBackupStorageRefs().size()) {
                        self.setStatus(ImageStatus.Deleted);
                        self = dbf.updateAndRefresh(self);
                        logger.debug(String.format("successfully deleted the image[uuid:%s, name:%s] with deletion policy[%s]",
                                self.getUuid(), self.getName(), deletionPolicy));
                    }
                }

                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeImageStateMsg) {
            handle((APIChangeImageStateMsg) msg);
        } else if (msg instanceof APIExpungeImageMsg) {
            handle((APIExpungeImageMsg) msg);
        } else if (msg instanceof APIDeleteImageMsg) {
            handle((APIDeleteImageMsg) msg);
        } else if (msg instanceof APIUpdateImageMsg) {
            handle((APIUpdateImageMsg) msg);
        } else if (msg instanceof APIRecoverImageMsg) {
            handle((APIRecoverImageMsg) msg);
        } else if (msg instanceof APISyncImageSizeMsg) {
            handle((APISyncImageSizeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APISyncImageSizeMsg msg) {
        final APISyncImageSizeEvent evt = new APISyncImageSizeEvent(msg.getId());
        syncImageSize(null, new ReturnValueCompletion<ImageSize>(msg) {
            @Override
            public void success(ImageSize ret) {
                self = dbf.reload(self);
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIRecoverImageMsg msg) {
        List<String> toRecoverBsUuids;
        if (msg.getBackupStorageUuids() == null || msg.getBackupStorageUuids().isEmpty()) {
            toRecoverBsUuids = CollectionUtils.transformToList(self.getBackupStorageRefs(), new Function<String, ImageBackupStorageRefVO>() {
                @Override
                public String call(ImageBackupStorageRefVO arg) {
                    return arg.getStatus() == ImageStatus.Deleted ? arg.getBackupStorageUuid() : null;
                }
            });

            if (toRecoverBsUuids.isEmpty()) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the image[uuid:%s, name:%s] is not deleted on any backup storage",
                                self.getUuid(), self.getName())
                ));
            }
        } else {
            toRecoverBsUuids = new ArrayList<String>();
            for (final String bsUuid : msg.getBackupStorageUuids()) {
                ImageBackupStorageRefVO ref = CollectionUtils.find(self.getBackupStorageRefs(), new Function<ImageBackupStorageRefVO, ImageBackupStorageRefVO>() {
                    @Override
                    public ImageBackupStorageRefVO call(ImageBackupStorageRefVO arg) {
                        return bsUuid.equals(arg.getBackupStorageUuid()) ? arg : null;
                    }
                });

                if (ref == null) {
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("the image[uuid:%s, name:%s] is not on the backup storage[uuid:%s]",
                                    self.getUuid(), self.getName(), bsUuid)
                    ));
                }

                if (ref.getStatus() != ImageStatus.Deleted) {
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("the image[uuid:%s, name:%s]'s status[%s] is not Deleted on the backup storage[uuid:%s]",
                                    self.getUuid(), self.getName(), ref.getStatus(), bsUuid)
                    ));
                }

                toRecoverBsUuids.add(bsUuid);
            }
        }

        List<Object> refs = new ArrayList<>();
        for (ImageBackupStorageRefVO ref : self.getBackupStorageRefs()) {
            if (toRecoverBsUuids.contains(ref.getBackupStorageUuid())) {
                ref.setStatus(ImageStatus.Ready);
                refs.add(ref);
            }
        }

        self.setStatus(ImageStatus.Ready);
        refs.add(self);
        dbf.updateCollection(refs);
        self = dbf.reload(self);

        logger.debug(String.format("successfully recovered the image[uuid:%s, name:%s] on the backup storage%s",
                self.getUuid(), self.getName(), toRecoverBsUuids));
        APIRecoverImageEvent evt = new APIRecoverImageEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(final APIExpungeImageMsg msg) {
        List<String> bsUuids = new ArrayList<>();
        if (msg.getBackupStorageUuids() == null || msg.getBackupStorageUuids().isEmpty()) {
            bsUuids = CollectionUtils.transformToList(
                    self.getBackupStorageRefs(),
                    new Function<String, ImageBackupStorageRefVO>() {
                        @Override
                        public String call(ImageBackupStorageRefVO arg) {
                            return ImageStatus.Deleted == arg.getStatus() ? arg.getBackupStorageUuid() : null;
                        }
                    }
            );

            if (bsUuids.isEmpty()) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the image[uuid:%s, name:%s] is not deleted on any backup storage",
                                self.getUuid(), self.getName())
                ));
            }
        } else {
            for (final String bsUuid : msg.getBackupStorageUuids()) {
                ImageBackupStorageRefVO ref = CollectionUtils.find(
                        self.getBackupStorageRefs(),
                        new Function<ImageBackupStorageRefVO, ImageBackupStorageRefVO>() {
                            @Override
                            public ImageBackupStorageRefVO call(ImageBackupStorageRefVO arg) {
                                return arg.getBackupStorageUuid().equals(bsUuid) ? arg : null;
                            }
                        }
                );

                if (ref == null) {
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("the image[uuid:%s, name:%s] is not on the backup storage[uuid:%s]",
                                    self.getUuid(), self.getName(), bsUuid)
                    ));
                }

                if (ref.getStatus() != ImageStatus.Deleted) {
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("the image[uuid:%s, name:%s] is not deleted on the backup storage[uuid:%s]",
                                    self.getUuid(), self.getName(), bsUuid)
                    ));
                }

                bsUuids.add(bsUuid);
            }
        }

        List<ExpungeImageMsg> emsgs = CollectionUtils.transformToList(bsUuids, new Function<ExpungeImageMsg, String>() {
            @Override
            public ExpungeImageMsg call(String arg) {
                ExpungeImageMsg emsg = new ExpungeImageMsg();
                emsg.setBackupStorageUuid(arg);
                emsg.setImageUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(emsg, ImageConstant.SERVICE_ID, self.getUuid());
                return emsg;
            }
        });

        final List<String> finalBsUuids = bsUuids;
        final APIExpungeImageEvent evt = new APIExpungeImageEvent(msg.getId());
        bus.send(emsgs, new CloudBusListCallBack(msg) {
            @Override
            public void run(List<MessageReply> replies) {
                List<ExpungeImageMsg> failedExpungeImageMsgList = new ArrayList<ExpungeImageMsg>();
                replies.stream()
                        .filter(r -> !r.isSuccess())
                        .forEach(r ->
                                {
                                    failedExpungeImageMsgList.add(emsgs.get(replies.indexOf(r)));
                                    String bsUuid = finalBsUuids.get(replies.indexOf(r));
                                    logger.warn(String.format(
                                            "failed to expunge the image[uuid:%s, name:%s] on the backup storage[uuid:%s], %s",
                                            self.getUuid(), self.getName(), bsUuid, r.getError()));
                                }
                        );
                gcImage(failedExpungeImageMsgList);

                bus.publish(evt);
            }
        });
    }

    private void gcImage(List<ExpungeImageMsg> expungeImageMsgs) {
        if (expungeImageMsgs.isEmpty()) {
            return;
        }

        TimeBasedGCEphemeralContext<Void> context = new TimeBasedGCEphemeralContext<>();
        context.setName("gc-image-msgs");
        context.setInterval(10);
        context.setTimeUnit(TimeUnit.SECONDS);
        context.setRunner(new GCRunner() {
            @Override
            public void run(GCContext context, final GCCompletion completion) {
                bus.send(expungeImageMsgs, new CloudBusListCallBack() {
                    @Override
                    public void run(List<MessageReply> replies) {
                    }
                });
            }
        });
        gcf.scheduleImmediately(context);
    }

    private void handle(APIUpdateImageMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (msg.getSystem() != null) {
            self.setSystem(msg.getSystem());
            update = true;
        }
        if (msg.getGuestOsType() != null) {
            self.setGuestOsType(msg.getGuestOsType());
            update = true;
        }
        if (msg.getMediaType() != null) {
            self.setMediaType(ImageMediaType.valueOf(msg.getMediaType()));
            update = true;
        }
        if (msg.getFormat() != null) {
            self.setFormat(msg.getFormat());
            update = true;
        }
        if (msg.getPlatform() != null) {
            self.setPlatform(ImagePlatform.valueOf(msg.getPlatform()));
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateImageEvent evt = new APIUpdateImageEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APIChangeImageStateMsg msg) {
        ImageStateEvent sevt = ImageStateEvent.valueOf(msg.getStateEvent());
        if (sevt == ImageStateEvent.disable) {
            self.setState(ImageState.Disabled);
        } else {
            self.setState(ImageState.Enabled);
        }
        self = dbf.updateAndRefresh(self);

        APIChangeImageStateEvent evt = new APIChangeImageStateEvent(msg.getId());
        evt.setInventory(ImageInventory.valueOf(self));
        bus.publish(evt);
    }

    private void handle(APIDeleteImageMsg msg) {
        final APIDeleteImageEvent evt = new APIDeleteImageEvent(msg.getId());

        final String issuer = ImageVO.class.getSimpleName();
        ImageDeletionStruct struct = new ImageDeletionStruct();
        struct.setImage(ImageInventory.valueOf(self));
        struct.setBackupStorageUuids(msg.getBackupStorageUuids());
        final List<ImageDeletionStruct> ctx = Arrays.asList(struct);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-image-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.publish(evt);
            }
        }).start();
    }
}
