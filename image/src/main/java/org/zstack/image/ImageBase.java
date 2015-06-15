package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.*;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    protected  ImageVO self;

    ImageBase(ImageVO vo) {
        self = vo;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage)msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    @Override
    public void deleteHook() {
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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final ImageDeletionMsg msg) {
        if (self.getStatus() == ImageStatus.Ready) {
            final boolean deleteAll = msg.getBackupStorageUuids() == null;

            final List<ImageBackupStorageRefVO> toDelete = CollectionUtils.transformToList(self.getBackupStorageRefs(), new Function<ImageBackupStorageRefVO, ImageBackupStorageRefVO>() {
                @Override
                public ImageBackupStorageRefVO call(ImageBackupStorageRefVO arg) {
                    if (deleteAll) {
                        return arg;
                    } else {
                        return msg.getBackupStorageUuids().contains(arg.getBackupStorageUuid()) ? arg : null;
                    }
                }
            });

            List<DeleteBitsOnBackupStorageMsg> dmsgs = CollectionUtils.transformToList(toDelete, new Function<DeleteBitsOnBackupStorageMsg, ImageBackupStorageRefVO>() {
                @Override
                public DeleteBitsOnBackupStorageMsg call(ImageBackupStorageRefVO arg) {
                    DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
                    dmsg.setBackupStorageUuid(arg.getBackupStorageUuid());
                    dmsg.setInstallPath(arg.getInstallPath());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dmsg.getBackupStorageUuid());
                    return dmsg;
                }
            });

            bus.send(dmsgs, new CloudBusListCallBack() {
                @Override
                public void run(List<MessageReply> replies) {
                    for (MessageReply reply : replies) {
                        ImageBackupStorageRefVO ref = toDelete.get(replies.indexOf(reply));
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to delete image[uuid:%s, name:%s] from backup storage[uuid:%s] because %s, need to garbage collect it",
                                    self.getUuid(), self.getName(), reply.getError(), ref.getBackupStorageUuid()));
                        } else {
                            logger.debug(String.format("successfully deleted image[uuid:%s, name:%s] from backup storage[uuid:%s]",
                                    self.getUuid(), self.getName(), ref.getBackupStorageUuid()));
                        }
                    }
                }
            });

            dbf.removeCollection(toDelete, ImageBackupStorageRefVO.class);
        } else {
            logger.warn(String.format("image[name: %s, uuid:%s] is deleted when downloading or creating, it will be garbage collected later", self.getName(), self.getUuid()));
        }

        deleteHook();

        ImageDeletionReply reply = new ImageDeletionReply();
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeImageStateMsg) {
            handle((APIChangeImageStateMsg) msg);
        } else if (msg instanceof APIDeleteImageMsg) {
            handle((APIDeleteImageMsg) msg);
        } else if (msg instanceof APIUpdateImageMsg) {
            handle((APIUpdateImageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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
