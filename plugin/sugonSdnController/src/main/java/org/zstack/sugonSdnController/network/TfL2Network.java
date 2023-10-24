package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.sugonSdnController.controller.SugonSdnController;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.L2NoVlanNetwork;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.sdnController.header.SdnControllerVO_;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

public class TfL2Network extends L2NoVlanNetwork implements TfL2NetworkExtensionPoint{

    @Autowired
    SdnControllerManager sdnControllerManager;

    public TfL2Network(L2NetworkVO self) {
        super(self);
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

    private void handleLocalMessage(Message msg) {
        if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else if (msg instanceof CheckL2NetworkOnHostMsg) {
            handle((CheckL2NetworkOnHostMsg) msg);
        } else if (msg instanceof PrepareL2NetworkOnHostMsg) {
            handle((PrepareL2NetworkOnHostMsg) msg);
        } else if (msg instanceof DetachL2NetworkFromClusterMsg) {
            handle((DetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof DeleteL2NetworkMsg) {
            handle((DeleteL2NetworkMsg) msg);
        } else if (msg instanceof L2NetworkDetachFromClusterMsg) {
            handle((L2NetworkDetachFromClusterMsg) msg);
        } else  {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(L2NetworkDetachFromClusterMsg msg) {
        L2NetworkDetachFromClusterReply reply = new L2NetworkDetachFromClusterReply();
        SQL.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid()).delete();
        self = dbf.reload(self);
        bus.reply(msg, reply);
    }

    private void handle(DeleteL2NetworkMsg msg) {
        DeleteL2NetworkReply reply = new DeleteL2NetworkReply();
        deleteTfL2NetworkOnSdnController(self, new Completion(msg) {
            @Override
            public void success() {
                SQL.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).delete();
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(DetachL2NetworkFromClusterMsg msg) {
        DetachL2NetworkFromClusterReply reply = new DetachL2NetworkFromClusterReply();
        SQL.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .delete();
        self = dbf.reload(self);
        bus.reply(msg, reply);
    }

    private void handle(PrepareL2NetworkOnHostMsg msg) {
        PrepareL2NetworkOnHostReply reply = new PrepareL2NetworkOnHostReply();
        bus.reply(msg, reply);
    }

    private void handle(CheckL2NetworkOnHostMsg msg) {
        CheckL2NetworkOnHostReply reply = new CheckL2NetworkOnHostReply();
        bus.reply(msg, reply);
    }

    private void handle(L2NetworkDeletionMsg msg) {
        L2NetworkDeletionReply reply = new L2NetworkDeletionReply();
        deleteTfL2NetworkOnSdnController(self, new Completion(msg) {
            @Override
            public void success() {
                SQL.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).delete();
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                bus.reply(msg, reply);
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL2NetworkMsg) {
            handle((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIUpdateL2NetworkMsg) {
            handle((APIUpdateL2NetworkMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDetachL2NetworkFromClusterMsg msg) {
        APIDetachL2NetworkFromClusterEvent evt = new APIDetachL2NetworkFromClusterEvent(msg.getId());
        SQL.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid()).delete();
        self = dbf.reload(self);
        evt.setInventory(self.toInventory());
        bus.publish(evt);
    }

    private void handle(APIAttachL2NetworkToClusterMsg msg){
        APIAttachL2NetworkToClusterEvent evt = new APIAttachL2NetworkToClusterEvent(msg.getId());
        L2NetworkClusterRefVO vo = new L2NetworkClusterRefVO();
        vo.setL2NetworkUuid(msg.getL2NetworkUuid());
        vo.setClusterUuid(msg.getClusterUuid());
        dbf.persist(vo);
        self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
        evt.setInventory(self.toInventory());
        bus.publish(evt);
    }

    private void handle(APIUpdateL2NetworkMsg msg){
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (!update) {
            return;
        }
        APIUpdateL2NetworkEvent evt = new APIUpdateL2NetworkEvent(msg.getId());
        updateTfL2NetworkOnSdnController(self, new Completion(msg) {
            @Override
            public void success() {
                self = dbf.updateAndRefresh(self);
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

    private void handle(APIDeleteL2NetworkMsg msg) {
        APIDeleteL2NetworkEvent evt = new APIDeleteL2NetworkEvent(msg.getId());
        deleteTfL2NetworkOnSdnController(self, new Completion(msg) {
            @Override
            public void success() {
                SQL.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).delete();
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                bus.publish(evt);
            }
        });
    }

    @Override
    public void createTfL2NetworkOnSdnController(L2NetworkVO l2NetworkVO, APICreateL2NetworkMsg msg, Completion completion) {
        if(l2NetworkVO.getPhysicalInterface() == null){
            l2NetworkVO.setPhysicalInterface(l2NetworkVO.getUuid());
        }
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.createL2Network(l2NetworkVO, msg, null, completion);
    }

    @Override
    public void deleteTfL2NetworkOnSdnController(L2NetworkVO l2NetworkVO, Completion completion) {
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.deleteL2Network(l2NetworkVO, null, completion);
    }

    @Override
    public void updateTfL2NetworkOnSdnController(L2NetworkVO l2NetworkVO, Completion completion) {
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
        sugonSdnController.updateL2Network(l2NetworkVO, null, completion);
    }
}
