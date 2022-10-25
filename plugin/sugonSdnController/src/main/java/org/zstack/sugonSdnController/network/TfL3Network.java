package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.sugonSdnController.controller.SugonSdnController;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.*;
import org.zstack.network.l3.L3BasicNetwork;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.sdnController.header.SdnControllerVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.err;

/**
 * @description:
 * @author: liupt@sugon.com
 * @create: 2022-10-11
 **/
public class TfL3Network extends L3BasicNetwork {
    private static final CLogger logger = Utils.getLogger(TfL3Network.class);
    @Autowired
    SdnControllerManager sdnControllerManager;

    public TfL3Network(L3NetworkVO self) {
        super(self);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIDeleteL3NetworkMsg) {
            handle((APIDeleteL3NetworkMsg) msg);
        } else if (msg instanceof APIUpdateL3NetworkMsg) {
            handle((APIUpdateL3NetworkMsg) msg);
        } else if (msg instanceof APIAddDnsToL3NetworkMsg) {
            handle((APIAddDnsToL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveDnsFromL3NetworkMsg) {
            handle((APIRemoveDnsFromL3NetworkMsg) msg);
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            handle((APIAddIpRangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIDeleteIpRangeMsg) {
            handle((APIDeleteIpRangeMsg) msg);
        } else if (msg instanceof APIAddHostRouteToL3NetworkMsg) {
            handle((APIAddHostRouteToL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveHostRouteFromL3NetworkMsg) {
            handle((APIRemoveHostRouteFromL3NetworkMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(APIDeleteL3NetworkMsg msg){
        APIDeleteL3NetworkEvent evt = new APIDeleteL3NetworkEvent(msg.getId());
        // 查询zstack数据库获取三层网络信息
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getUuid()).find();
        if(l3Network!=null){
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
            SdnController sdnController = sdnControllerManager.getSdnController(sdn);
            SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
            sugonSdnController.deleteL3Network(l3Network, new Completion(msg){
                @Override
                public void success() {
                    // 执行zstack逻辑
                    TfL3Network.super.handleMessage(msg);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                    bus.publish(evt);
                }
            });

        } else{
            evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, "L3 Network is missing"));
            bus.publish(evt);
        }
    }

    private void handle(APIUpdateL3NetworkMsg msg){
        APIUpdateL3NetworkEvent evt = new APIUpdateL3NetworkEvent(msg.getId());
        // 查询zstack数据库获取三层网络信息
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getUuid()).find();
        if(l3Network!=null){
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
            SdnController sdnController = sdnControllerManager.getSdnController(sdn);
            SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
            sugonSdnController.updateL3Network(l3Network,msg, new Completion(msg){
                @Override
                public void success() {
                    // 执行zstack逻辑
                    TfL3Network.super.handleMessage(msg);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                    bus.publish(evt);
                }
            });

        } else{
            evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, "L3 Network is missing"));
            bus.publish(evt);
        }

    }

    private void handle(APIAddIpRangeByNetworkCidrMsg msg){
        APIAddIpRangeByNetworkCidrEvent evt = new APIAddIpRangeByNetworkCidrEvent(msg.getId());
        // 查询zstack数据库获取三层网络信息
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
            SdnController sdnController = sdnControllerManager.getSdnController(sdn);
            SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
            sugonSdnController.addL3IpRangeByCidr(l3Network,msg, new Completion(msg){
                @Override
                public void success() {
                    // 执行zstack逻辑
                    TfL3Network.super.handleMessage(msg);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                    bus.publish(evt);
                }
            });
        } else{
            evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, "L3 Network is missing"));
            bus.publish(evt);
        }
    }

    private void handle(APIDeleteIpRangeMsg msg){
        APIDeleteIpRangeEvent evt = new APIDeleteIpRangeEvent(msg.getId());
        // 查询zstack数据库获取三层网络信息
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
            SdnController sdnController = sdnControllerManager.getSdnController(sdn);
            SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
            sugonSdnController.deleteL3Network(l3Network, new Completion(msg){
                @Override
                public void success() {
                    // 执行zstack逻辑
                    TfL3Network.super.handleMessage(msg);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                    bus.publish(evt);
                }
            });

        } else{
            evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, "L3 Network is missing"));
            bus.publish(evt);
        }
    }

    private void handle(APIAddDnsToL3NetworkMsg msg){
        APIAddDnsToL3NetworkEvent evt = new APIAddDnsToL3NetworkEvent(msg.getId());
        // 查询zstack数据库获取三层网络信息
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
            SdnController sdnController = sdnControllerManager.getSdnController(sdn);
            SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
            sugonSdnController.addL3Dns(l3Network, msg,new Completion(msg){
                @Override
                public void success() {
                    // 执行zstack逻辑
                    TfL3Network.super.handleMessage(msg);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                    bus.publish(evt);
                }
            });

        } else{
            evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, "L3 Network is missing"));
            bus.publish(evt);
        }
    }

    private void handle(APIRemoveDnsFromL3NetworkMsg msg){
        APIRemoveDnsFromL3NetworkEvent evt = new APIRemoveDnsFromL3NetworkEvent(msg.getId());
        // 查询zstack数据库获取三层网络信息
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
            SdnController sdnController = sdnControllerManager.getSdnController(sdn);
            SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
            sugonSdnController.deleteL3Dns(l3Network, msg,new Completion(msg){
                @Override
                public void success() {
                    // 执行zstack逻辑
                    TfL3Network.super.handleMessage(msg);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                    bus.publish(evt);
                }
            });

        } else{
            evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, "L3 Network is missing"));
            bus.publish(evt);
        }
    }

    private void handle(APIAddHostRouteToL3NetworkMsg msg){
        APIAddHostRouteToL3NetworkEvent evt = new APIAddHostRouteToL3NetworkEvent(msg.getId());
        // 查询zstack数据库获取三层网络信息
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
            SdnController sdnController = sdnControllerManager.getSdnController(sdn);
            SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
            sugonSdnController.addL3HostRoute(l3Network, msg,new Completion(msg){
                @Override
                public void success() {
                    // 执行zstack逻辑
                    TfL3Network.super.handleMessage(msg);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                    bus.publish(evt);
                }
            });

        } else{
            evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, "L3 Network is missing"));
            bus.publish(evt);
        }
    }

    private void handle(APIRemoveHostRouteFromL3NetworkMsg msg){
        APIRemoveHostRouteFromL3NetworkEvent evt = new APIRemoveHostRouteFromL3NetworkEvent(msg.getId());
        // 查询zstack数据库获取三层网络信息
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
            SdnController sdnController = sdnControllerManager.getSdnController(sdn);
            SugonSdnController sugonSdnController = (SugonSdnController) sdnController;
            sugonSdnController.deleteL3HostRoute(l3Network, msg,new Completion(msg){
                @Override
                public void success() {
                    // 执行zstack逻辑
                    TfL3Network.super.handleMessage(msg);
//                    bus.publish(evt);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                    bus.publish(evt);
                }
            });

        } else{
            evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, "L3 Network is missing"));
            bus.publish(evt);
        }
    }

}
