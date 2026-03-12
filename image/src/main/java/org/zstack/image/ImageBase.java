package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.IsoOperator;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.identity.SharedResourceVO;
import org.zstack.header.identity.SharedResourceVO_;
import org.zstack.header.image.APICalculateImageHashEvent;
import org.zstack.header.image.APICalculateImageHashMsg;
import org.zstack.header.image.APIChangeImageStateEvent;
import org.zstack.header.image.APIChangeImageStateMsg;
import org.zstack.header.image.APIDeleteImageEvent;
import org.zstack.header.image.APIDeleteImageMsg;
import org.zstack.header.image.APIExpungeImageEvent;
import org.zstack.header.image.APIExpungeImageMsg;
import org.zstack.header.image.APIRecoverImageEvent;
import org.zstack.header.image.APIRecoverImageMsg;
import org.zstack.header.image.APISetImageBootModeEvent;
import org.zstack.header.image.APISetImageBootModeMsg;
import org.zstack.header.image.APISyncImageSizeEvent;
import org.zstack.header.image.APISyncImageSizeMsg;
import org.zstack.header.image.APIUpdateImageEvent;
import org.zstack.header.image.APIUpdateImageMsg;
import org.zstack.header.image.AddImageMsg;
import org.zstack.header.image.CalculateImageHashMsg;
import org.zstack.header.image.CalculateImageHashReply;
import org.zstack.header.image.CancelAddImageMsg;
import org.zstack.header.image.CancelDownloadImageMsg;
import org.zstack.header.image.CancelDownloadImageReply;
import org.zstack.header.image.ExpungeImageExtensionPoint;
import org.zstack.header.image.ExpungeImageMsg;
import org.zstack.header.image.ExpungeImageReply;
import org.zstack.header.image.GetImageEncryptedMsg;
import org.zstack.header.image.Image;
import org.zstack.header.image.ImageArchitecture;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageDeletionMsg;
import org.zstack.header.image.ImageDeletionPolicyManager;
import org.zstack.header.image.ImageDeletionReply;
import org.zstack.header.image.ImageDeletionStruct;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageStateEvent;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.image.SyncImageSizeMsg;
import org.zstack.header.image.SyncImageSizeReply;
import org.zstack.header.image.SyncSystemTagFromTagMsg;
import org.zstack.header.image.SyncSystemTagFromTagReply;
import org.zstack.header.image.SyncSystemTagFromVolumeMsg;
import org.zstack.header.image.SyncSystemTagFromVolumeReply;
import org.zstack.header.image.UpdateImageMsg;
import org.zstack.header.image.UpdateImageReply;
import org.zstack.header.image.GetImageEncryptedReply;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.backup.CalculateImageHashOnBackupStorageMsg;
import org.zstack.header.storage.backup.CalculateImageHashOnBackupStorageReply;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.backup.GetImageEncryptedOnBackupStorageMsg;
import org.zstack.header.storage.backup.GetImageEncryptedOnBackupStorageReply;
import org.zstack.header.storage.backup.ReturnBackupStorageMsg;
import org.zstack.header.storage.backup.SyncImageSizeOnBackupStorageMsg;
import org.zstack.header.storage.backup.SyncImageSizeOnBackupStorageReply;
import org.zstack.header.vm.DetachIsoFromVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.volume.VolumeType;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 5:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ImageBase implements Image {
    private static final CLogger logger = Utils.getLogger(ImageBase.class);

    protected String syncThreadId;
    @Autowired
    private ThreadFacade thdf;
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
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;

    protected ImageVO self;

    public ImageBase(ImageVO vo) {
        self = vo;
        syncThreadId = String.format("image-%s", self.getUuid());
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
        } else if (msg instanceof CancelAddImageMsg) {
            handle((CancelAddImageMsg) msg);
        } else if (msg instanceof ExpungeImageMsg) {
            handle((ExpungeImageMsg) msg);
        } else if (msg instanceof SyncImageSizeMsg) {
            handle((SyncImageSizeMsg) msg);
        } else if (msg instanceof OverlayMessage) {
            handle((OverlayMessage) msg);
        } else if (msg instanceof SyncSystemTagFromVolumeMsg) {
            handle((SyncSystemTagFromVolumeMsg) msg);
        } else if (msg instanceof SyncSystemTagFromTagMsg) {
            handle((SyncSystemTagFromTagMsg) msg);
        } else if (msg instanceof UpdateImageMsg) {
            handle((UpdateImageMsg) msg);
        } else if (msg instanceof GetImageEncryptedMsg) {
            handle((GetImageEncryptedMsg) msg);
        } else if (msg instanceof CalculateImageHashMsg) {
            handle((CalculateImageHashMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(GetImageEncryptedMsg msg) {
        GetImageEncryptedReply reply = new GetImageEncryptedReply();
        getImageMd5(msg, new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String encrypted) {
                reply.setEncrypt(encrypted);
                reply.setImageUuid(msg.getImageUuid());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });

    }

    private void handle(CalculateImageHashMsg msg) {
        CalculateImageHashReply reply = new CalculateImageHashReply();
        if (self.getMd5Sum() != null) {
            bus.reply(msg, reply);
            return;
        }

        CalculateImageHashOnBackupStorageMsg cmsg = new CalculateImageHashOnBackupStorageMsg();
        cmsg.setImageUuid(msg.getUuid());
        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setAlgorithm(msg.getAlgorithm());
        bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                    bus.reply(msg, reply);
                    return;
                }

                CalculateImageHashOnBackupStorageReply breply = r.castReply();
                SQL.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).set(ImageVO_.md5Sum, breply.getHashValue()).update();
                bus.reply(msg, reply);
            }
        });
    }

    private void getImageMd5(GetImageEncryptedMsg msg, final ReturnValueCompletion<String> completion) {
        GetImageEncryptedOnBackupStorageMsg backupStorageMsg = new GetImageEncryptedOnBackupStorageMsg();
        String backupStorageUuid = Q.New(ImageBackupStorageRefVO.class)
                .select(ImageBackupStorageRefVO_.backupStorageUuid)
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .findValue();
        backupStorageMsg.setImageUuid(msg.getImageUuid());
        backupStorageMsg.setBackupStorageUuid(backupStorageUuid);

        bus.makeTargetServiceIdByResourceUuid(backupStorageMsg, BackupStorageConstant.SERVICE_ID, backupStorageUuid);
        bus.send(backupStorageMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    GetImageEncryptedOnBackupStorageReply sr = reply.castReply();
                    completion.success(sr.getEncrypted());
                }
            }
        });
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
                throw new OperationFailureException(operr(ORG_ZSTACK_IMAGE_10038, "the image[uuid:%s, name:%s] is not on any backup storage", self.getUuid(), self.getName()));
            }

            SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
            q.select(BackupStorageVO_.uuid);
            q.add(BackupStorageVO_.uuid, Op.IN, bsUuids);
            q.add(BackupStorageVO_.status, Op.EQ, BackupStorageStatus.Connected);
            q.setLimit(1);
            backupStorageUuid = q.findValue();
            if (backupStorageUuid == null) {
                completion.fail(operr(ORG_ZSTACK_IMAGE_10039, "No connected backup storage found for image[uuid:%s, name:%s]",
                        self.getUuid(), self.getName()));
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
                arg -> arg.getBackupStorageUuid().equals(msg.getBackupStorageUuid()) ? arg : null
        );

        if (ref == null) {
            logger.debug(String.format("cannot find reference for the image[uuid:%s] on the backup storage[uuid:%s], assume it's been deleted",
                    self.getUuid(), msg.getBackupStorageUuid()));
            bus.reply(msg, reply);
            return;
        }
        dbf.remove(ref);

        DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
        dmsg.setBackupStorageUuid(ref.getBackupStorageUuid());
        dmsg.setInstallPath(ref.getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dmsg.getBackupStorageUuid());
        bus.send(dmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    BackupStorageDeleteBitGC gc = new BackupStorageDeleteBitGC();
                    gc.NAME = String.format("gc-delete-bits-%s-on-backup-storage-%s", msg.getImageUuid(), ref.getBackupStorageUuid());
                    gc.backupStorageUuid = ref.getBackupStorageUuid();
                    gc.imageUuid = msg.getImageUuid();
                    gc.installPath = ref.getInstallPath();
                    gc.submit(ImageGlobalConfig.DELETION_GARBAGE_COLLECTION_INTERVAL.value(Long.class),
                            TimeUnit.SECONDS);
                }

                returnBackupStorageCapacity(ref.getBackupStorageUuid(), self.getActualSize());

                //TODO remove ref from metadata, this logic should after all refs deleted
                runAfterExpungeImageExtension(ref.getBackupStorageUuid());

                logger.debug(String.format("successfully expunged the image[uuid: %s, name: %s] on the backup storage[uuid: %s]",
                        self.getUuid(), self.getName(), ref.getBackupStorageUuid()));

                new SQLBatch() {
                    // delete the image if it's not on any backup storage
                    @Override
                    protected void scripts() {
                        long count = sql("select count(ref) from ImageBackupStorageRefVO ref" +
                                " where ref.imageUuid = :uuid", Long.class)
                                .param("uuid", msg.getImageUuid()).find();

                        if (count == 0) {
                            // the image is expunged on all backup storage
                            sql(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).delete();
                            sql(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, msg.getImageUuid()).delete();

                            logger.debug(String.format("the image[uuid:%s, name:%s] has been expunged on all backup storage, remove it from database",
                                    self.getUuid(), self.getName()));
                        }
                    }
                }.execute();

                bus.reply(msg, reply);
            }
        });
    }

    private void returnBackupStorageCapacity(final String bsUuid, final long size) {
        ReturnBackupStorageMsg msg = new ReturnBackupStorageMsg();
        msg.setBackupStorageUuid(bsUuid);
        msg.setSize(size);
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, bsUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to return capacity[%s] to the backup storage[uuid:%s], %s", size, bsUuid, reply.getError()));
                }
            }
        });
    }

    private void handle(final ImageDeletionMsg msg) {
        final ImageDeletionReply reply = new ImageDeletionReply();
        Set<ImageBackupStorageRefVO> bsRefs = self.getBackupStorageRefs();

        if (bsRefs.isEmpty()) {
            if (self.getStatus() == ImageStatus.Ready) {
                SQL.New(ImageVO.class).eq(ImageVO_.uuid, self.getUuid()).delete();
            } else {
                SQL.New(ImageVO.class).eq(ImageVO_.uuid, self.getUuid()).hardDelete();
            }
            bus.reply(msg, reply);
            return;
        } else if (bsRefs.stream().allMatch(
                r -> r.getStatus() == ImageStatus.Creating || r.getStatus() == ImageStatus.Downloading)) {
            // the image is not on any backup storage; mostly likely the image is not in the status of Ready, for example
            // it's still downloading
            // in this case, we directly delete it from the database
            new SQLBatch() {
                @Override
                protected void scripts() {
                    // in case 'recover api' called for an incomplete image
                    sql(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.imageUuid, self.getUuid()).hardDelete();
                    sql(ImageVO.class).eq(ImageVO_.uuid, self.getUuid()).hardDelete();
                }
            }.execute();

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

        // DEBT: NoRollbackFlow — in scripts
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> vmUuids = IsoOperator.getVmUuidByIsoUuid(msg.getImageUuid());
                if (vmUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<DetachIsoFromVmInstanceMsg> detachIsoFromVmInstanceMsgs = new ArrayList<>();
                for (String vmUuid : vmUuids) {
                    DetachIsoFromVmInstanceMsg detachIsoFromVmInstanceMsg = new DetachIsoFromVmInstanceMsg();
                    detachIsoFromVmInstanceMsg.setVmInstanceUuid(vmUuid);
                    detachIsoFromVmInstanceMsg.setIsoUuid(msg.getImageUuid());
                    bus.makeLocalServiceId(detachIsoFromVmInstanceMsg, VmInstanceConstant.SERVICE_ID);
                    detachIsoFromVmInstanceMsgs.add(detachIsoFromVmInstanceMsg);
                }

                List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<ErrorCode>());
                new While<>(detachIsoFromVmInstanceMsgs).all((detachIsoFromVmInstanceMsg, completion) -> {
                    bus.send(detachIsoFromVmInstanceMsg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply rly) {
                            if (!rly.isSuccess()) {
                                errors.add(rly.getError());
                            }

                            completion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errors.size() != 0) {
                            trigger.fail(operr(ORG_ZSTACK_IMAGE_10040, "detach iso[uuid=%s] from vm failed, errors are %s"
                                    ,msg.getImageUuid(), JSONObjectUtil.toJsonString(errors)));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        });

        List<Object> refs = new ArrayList<>();
        for (final ImageBackupStorageRefVO ref : toDelete) {
            // DEBT: NoRollbackFlow — reason TBD
            chain.then(new NoRollbackFlow() {
                String __name__ = String.format("delete-image-%s-from-backup-storage-%s", self.getUuid(), ref.getBackupStorageUuid());

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    if (deletionPolicy == ImageDeletionPolicy.Direct) {
                        dbf.remove(ref);

                        DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
                        dmsg.setBackupStorageUuid(ref.getBackupStorageUuid());
                        dmsg.setInstallPath(ref.getInstallPath());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dmsg.getBackupStorageUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete image[uuid:%s, name:%s] from backup storage[uuid:%s] because %s," +
                                                    " need to garbage collect it",
                                            self.getUuid(), self.getName(), reply.getError(), ref.getBackupStorageUuid()));

                                    BackupStorageDeleteBitGC gc = new BackupStorageDeleteBitGC();
                                    gc.NAME = String.format("gc-delete-bits-%s-on-backup-storage-%s", msg.getImageUuid(), ref.getBackupStorageUuid());
                                    gc.backupStorageUuid = ref.getBackupStorageUuid();
                                    gc.imageUuid = msg.getImageUuid();
                                    gc.installPath = ref.getInstallPath();
                                    gc.deduplicateSubmit(ImageGlobalConfig.DELETION_GARBAGE_COLLECTION_INTERVAL.value(Long.class),
                                            TimeUnit.SECONDS);
                                }

                                returnBackupStorageCapacity(ref.getBackupStorageUuid(), self.getActualSize());
                                // now delete ref in metadata
                                runAfterExpungeImageExtension(ref.getBackupStorageUuid());
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
                        refs.add(ref);
                        trigger.next();
                    }
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        for (Object ref : refs) {
                            // update ref status if there is any
                            dbf.getEntityManager().merge(ref);
                        }

                        dbf.getEntityManager().flush();

                        self = dbf.getEntityManager().find(ImageVO.class, self.getUuid());

                        if (self.getBackupStorageRefs().isEmpty()) {
                            // the image is directly deleted from all backup storage
                            // hard delete it
                            sql(ImageVO.class).eq(ImageVO_.uuid, self.getUuid()).delete();

                            if (deletionPolicy == ImageDeletionPolicy.DeleteReference) {
                                logger.debug(String.format("successfully directly deleted the image[uuid:%s, name:%s] from the database," +
                                        " as the policy is DeleteReference, it's still on the physical backup storage", self.getUuid(), self.getName()));
                            } else {
                                logger.debug(String.format("successfully directly deleted the image[uuid:%s, name:%s]", self.getUuid(), self.getName()));
                            }
                        } else {
                            if (self.getBackupStorageRefs().stream().noneMatch(r -> r.getStatus() != ImageStatus.Deleted)) {
                                self.setStatus(ImageStatus.Deleted);
                                dbf.getEntityManager().merge(self);

                                logger.debug(String.format("successfully deleted the image[uuid:%s, name:%s] with deletion policy[%s]",
                                        self.getUuid(), self.getName(), deletionPolicy));
                            }
                        }
                    }
                }.execute();

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

    private void handle(CancelAddImageMsg msg) {
        CancelDownloadImageReply reply = new CancelDownloadImageReply();

        AddImageMsg amsg = msg.getMsg();
        List<String> bsUuids = amsg.getBackupStorageUuids();
        ImageInventory img = ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class));
        ErrorCodeList err = new ErrorCodeList();
        new While<>(bsUuids).all((bsUuid, compl) -> {
            CancelDownloadImageMsg cmsg = new CancelDownloadImageMsg();
            cmsg.setImageInventory(img);
            cmsg.setBackupStorageUuid(bsUuid);
            cmsg.setCancellationApiId(msg.getCancellationApiId());
            bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
            bus.send(cmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply r) {
                    if (!r.isSuccess()) {
                        err.getCauses().add(r.getError());
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!err.getCauses().isEmpty()) {
                    reply.setError(err.getCauses().get(0));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(OverlayMessage msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doOverlayMessage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "overlay-message";
            }
        });
    }

    private void doOverlayMessage(OverlayMessage msg, NoErrorCompletion noErrorCompletion) {
        bus.send(msg.getMessage(), new CloudBusCallBack(msg, noErrorCompletion) {
            @Override
            public void run(MessageReply reply) {
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }

    private void runAfterExpungeImageExtension(String backupStorageUuid) {
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(ExpungeImageExtensionPoint.class),
                ext -> ext.afterExpungeImage(ImageInventory.valueOf(self), backupStorageUuid));
    }

    private void handle(SyncSystemTagFromVolumeMsg msg) {
        SyncSystemTagFromVolumeReply reply = new SyncSystemTagFromVolumeReply();

        // only sync root volume
        List<String> vmSystemTags = SQL.New("select s.tag from SystemTagVO s, VolumeVO vol" +
                " where vol.uuid = :volUuid" +
                " and vol.type = :type" +
                " and vol.vmInstanceUuid = s.resourceUuid", String.class)
                .param("volUuid", msg.getVolumeUuid())
                .param("type", VolumeType.Root)
                .list();

        syncVmSystemTags(vmSystemTags);
        bus.reply(msg, reply);
    }

    private void handle(SyncSystemTagFromTagMsg msg) {
        SyncSystemTagFromTagReply reply = new SyncSystemTagFromTagReply();
        syncVmSystemTags(msg.getVmSystemTags());
        bus.reply(msg, reply);
    }

    private void syncVmSystemTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        for (String tag : tags) {
            if (VmSystemTags.VM_INJECT_QEMUGA.getTagFormat().equals(tag)) {
                tagMgr.createNonInherentSystemTag(self.getUuid(),
                        ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat(),
                        ImageVO.class.getSimpleName());
            } else if (VmSystemTags.BOOT_MODE.isMatch(tag)) {
                String bootMode = VmSystemTags.BOOT_MODE.getTokenByTag(tag, VmSystemTags.BOOT_MODE_TOKEN);
                SystemTagCreator creator = ImageSystemTags.BOOT_MODE.newSystemTagCreator(self.getUuid());
                creator.setTagByTokens(Collections.singletonMap(VmSystemTags.BOOT_MODE_TOKEN, bootMode));
                creator.inherent = false;
                creator.recreate = true;
                creator.create();
            } else if (VmSystemTags.VM_GUEST_TOOLS.isMatch(tag)) {
                String guestTools = VmSystemTags.VM_GUEST_TOOLS.getTokenByTag(tag, VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN);
                SystemTagCreator creator = ImageSystemTags.IMAGE_GUEST_TOOLS.newSystemTagCreator(self.getUuid());
                creator.setTagByTokens(Collections.singletonMap(ImageSystemTags.IMAGE_GUEST_TOOLS_VERSION_TOKEN, guestTools));
                creator.inherent = false;
                creator.recreate = true;
                creator.create();
            }
        }
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
        } else if (msg instanceof APISetImageBootModeMsg) {
            handle((APISetImageBootModeMsg) msg);
        } else if (msg instanceof APICalculateImageHashMsg) {
            handle((APICalculateImageHashMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APISetImageBootModeMsg msg) {
        SystemTagCreator creator = ImageSystemTags.BOOT_MODE.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(
                e(ImageSystemTags.BOOT_MODE_TOKEN, msg.getBootMode())
        ));
        creator.recreate = true;
        creator.create();
        APISetImageBootModeEvent evt = new APISetImageBootModeEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(final APICalculateImageHashMsg msg) {
        final APICalculateImageHashEvent evt = new APICalculateImageHashEvent(msg.getId());
        CalculateImageHashMsg cmsg = new CalculateImageHashMsg(msg);
        bus.makeTargetServiceIdByResourceUuid(cmsg, ImageConstant.SERVICE_ID, cmsg.getImageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }

                evt.setInventory(ImageInventory.valueOf(dbf.reload(self)));
                bus.publish(evt);
            }
        });
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
                evt.setError(errorCode);
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
                throw new OperationFailureException(operr(ORG_ZSTACK_IMAGE_10041, "the image[uuid:%s, name:%s] is not deleted on any backup storage",
                                self.getUuid(), self.getName()));
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
                    throw new OperationFailureException(argerr(ORG_ZSTACK_IMAGE_10042, "the image[uuid:%s, name:%s] is not on the backup storage[uuid:%s]",
                                    self.getUuid(), self.getName(), bsUuid));
                }

                if (ref.getStatus() != ImageStatus.Deleted) {
                    throw new OperationFailureException(argerr(ORG_ZSTACK_IMAGE_10043, "the image[uuid:%s, name:%s]'s status[%s] is not Deleted on the backup storage[uuid:%s]",
                                    self.getUuid(), self.getName(), ref.getStatus(), bsUuid));
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
                throw new OperationFailureException(operr(ORG_ZSTACK_IMAGE_10044, "the image[uuid:%s, name:%s] is not deleted on any backup storage",
                        self.getUuid(), self.getName()));
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
                    throw new OperationFailureException(argerr(ORG_ZSTACK_IMAGE_10045, "the image[uuid:%s, name:%s] is not on the backup storage[uuid:%s]",
                            self.getUuid(), self.getName(), bsUuid));
                }

                if (ref.getStatus() != ImageStatus.Deleted) {
                    throw new OperationFailureException(argerr(ORG_ZSTACK_IMAGE_10046, "the image[uuid:%s, name:%s] is not deleted on the backup storage[uuid:%s]",
                            self.getUuid(), self.getName(), bsUuid));
                }

                bsUuids.add(bsUuid);
            }
        }


        new While<>(bsUuids).all((bsUuid, completion) -> {
            ExpungeImageMsg emsg = new ExpungeImageMsg();
            emsg.setBackupStorageUuid(bsUuid);
            emsg.setImageUuid(self.getUuid());
            bus.makeTargetServiceIdByResourceUuid(emsg, ImageConstant.SERVICE_ID, self.getUuid());
            bus.send(emsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    }

                    completion.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.publish(new APIExpungeImageEvent(msg.getId()));
            }
        });
    }

    private void updateImage(UpdateImageMsg msg) {
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
        if (msg.getArchitecture() != null) {
            self.setArchitecture(msg.getArchitecture());
            update = true;
        }
        if (msg.getVirtio() != null) {
            self.setVirtio(msg.getVirtio());
            update = true;
        }

        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        if (ImageArchitecture.aarch64.toString().equals(msg.getArchitecture())){
            SystemTagCreator creator = ImageSystemTags.BOOT_MODE.newSystemTagCreator(msg.getImageUuid());
            creator.setTagByTokens(Collections.singletonMap(ImageSystemTags.BOOT_MODE_TOKEN, ImageBootMode.UEFI.toString()));
            creator.recreate = true;
            creator.create();
        }

        if (ImageArchitecture.x86_64.toString().equals(msg.getArchitecture()) && self.isSystem()) {
            SystemTagCreator creator = ImageSystemTags.BOOT_MODE.newSystemTagCreator(msg.getUuid());
            creator.setTagByTokens(Collections.singletonMap(ImageSystemTags.BOOT_MODE_TOKEN, ImageBootMode.Legacy.toString()));
            creator.recreate = true;
            creator.create();
        }
    }

    private void handle(UpdateImageMsg msg) {
        updateImage(msg);
        UpdateImageReply reply = new UpdateImageReply();
        reply.setInventory(getSelfInventory());
        bus.reply(msg, reply);
    }

    private void handle(APIUpdateImageMsg msg) {
        updateImage(UpdateImageMsg.valueOf(msg));

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
            // DEBT: NoRollbackFlow — reason TBD
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
            // DEBT: NoRollbackFlow — reason TBD
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
            // DEBT: NoRollbackFlow — reason TBD
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
                evt.setError(err(ORG_ZSTACK_IMAGE_10047, SysErrors.DELETE_RESOURCE_ERROR, errCode, errCode.getDetails()));
                bus.publish(evt);
            }
        }).start();
    }
}
