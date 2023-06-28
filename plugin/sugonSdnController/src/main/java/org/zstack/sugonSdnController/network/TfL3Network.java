package org.zstack.sugonSdnController.network;

import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
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
import org.zstack.sugonSdnController.controller.SugonSdnController;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortClient;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;
import java.io.IOException;
import static org.zstack.core.Platform.err;
import static org.zstack.utils.network.NetworkUtils.getSubnetInfo;

/**
 * @description:
 * @author: liupt@sugon.com
 * @create: 2022-10-11
 **/
public class TfL3Network extends L3BasicNetwork {
    private static final CLogger logger = Utils.getLogger(TfL3Network.class);
    @Autowired
    SdnControllerManager sdnControllerManager;

    @Autowired
    protected DatabaseFacade dbf;

    SugonSdnController sugonSdnController;

    public TfL3Network(L3NetworkVO self) {
        super(self);
    }

    private SugonSdnController getSugonSdnController() {
        if (sugonSdnController != null){
            return sugonSdnController;
        }
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).find();
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        return (SugonSdnController) sdnController;
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
        } else if (msg instanceof APICheckIpAvailabilityMsg) {
            handle((APICheckIpAvailabilityMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(APICheckIpAvailabilityMsg msg) {
        APICheckIpAvailabilityReply reply = new APICheckIpAvailabilityReply();
        CheckIpAvailabilityReply r = new CheckIpAvailabilityReply();
        TfPortClient tfPortClient = new TfPortClient();
        try {
            boolean availability = tfPortClient.checkTfIpAvailability(msg.getIp(), msg.getL3NetworkUuid());
            r.setAvailable(!availability);
            if (availability){
                r.setReason("IP address is already in use.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        reply.setAvailable(r.isAvailable());
        reply.setReason(r.getReason());
        bus.reply(msg, reply);
    }

    private void handle(APIDeleteL3NetworkMsg msg){
        APIDeleteL3NetworkEvent evt = new APIDeleteL3NetworkEvent(msg.getId());
        // Get L3 Network from zstack db
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getUuid()).find();
        if(l3Network!=null){
            getSugonSdnController().deleteL3Network(l3Network, new Completion(msg){
                @Override
                public void success() {
                    // zstack business processing
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
        // Get L3 Network from zstack db
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getUuid()).find();
        if(l3Network!=null){
            getSugonSdnController().updateL3Network(l3Network,msg, new Completion(msg){
                @Override
                public void success() {
                    // zstack business processing
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
        // Get L3 Network from zstack db
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            getSugonSdnController().addL3IpRangeByCidr(l3Network,msg, new Completion(msg){
                @Override
                public void success() {
                    // zstack business processing
                    TfL3Network.super.handleMessage(msg);
                    procReservedIP(msg);
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
        // Get L3 Network from zstack db
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            getSugonSdnController().deleteL3Network(l3Network, new Completion(msg){
                @Override
                public void success() {
                    // zstack business processing
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
        // Get L3 Network from zstack db
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            getSugonSdnController().addL3Dns(l3Network, msg,new Completion(msg){
                @Override
                public void success() {
                    // zstack business processing
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
        // Get L3 Network from zstack db
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            getSugonSdnController().deleteL3Dns(l3Network, msg,new Completion(msg){
                @Override
                public void success() {
                    // zstack business processing
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
        // Get L3 Network from zstack db
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            getSugonSdnController().addL3HostRoute(l3Network, msg,new Completion(msg){
                @Override
                public void success() {
                    // zstack business processing
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
        // Get L3 Network from zstack db
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if(l3Network!=null){
            getSugonSdnController().deleteL3HostRoute(l3Network, msg,new Completion(msg){
                @Override
                public void success() {
                    // zstack business processing
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

    private void procReservedIP(APIAddIpRangeByNetworkCidrMsg msg){
        SubnetUtils.SubnetInfo subnetInfo = getSubnetInfo(new SubnetUtils(msg.getNetworkCidr()));
        // Reserved for service
        String servIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnetInfo.getLowAddress())+1);
        // Reserved for edge
        String edgeIp = subnetInfo.getHighAddress();
        // Get IP Range
        IpRangeVO ipRangeVO = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid()).find();
        // Set Used Ip
        UsedIpVO servIpInfo = new UsedIpVO();
        servIpInfo.setUuid(Platform.getUuid());
        servIpInfo.setIpRangeUuid(ipRangeVO.getUuid());
        servIpInfo.setL3NetworkUuid(ipRangeVO.getL3NetworkUuid());
        servIpInfo.setIp(servIp);
        servIpInfo.setIpInLong(NetworkUtils.ipv4StringToLong(servIp));
        servIpInfo.setGateway(ipRangeVO.getGateway());
        servIpInfo.setNetmask(ipRangeVO.getNetmask());
        servIpInfo.setIpVersion(IPv6Constants.IPv4);
        servIpInfo.setLastOpDate(ipRangeVO.getLastOpDate());
        servIpInfo.setCreateDate(ipRangeVO.getCreateDate());
        dbf.persist(servIpInfo);
        UsedIpVO edgeIpInfo = new UsedIpVO();
        BeanUtils.copyProperties(servIpInfo,edgeIpInfo);
        edgeIpInfo.setUuid(Platform.getUuid());
        edgeIpInfo.setIp(edgeIp);
        edgeIpInfo.setIpInLong(NetworkUtils.ipv4StringToLong(edgeIp));
        dbf.persist(edgeIpInfo);
    }

}
