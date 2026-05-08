package org.zstack.sdnController;

import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.network.l3.SdnControllerL3;
import org.zstack.header.network.service.SdnControllerDhcp;
import org.zstack.header.network.sdncontroller.SdnControllerStatus;
import org.zstack.header.network.sdncontroller.SdnControllerStatusEvent;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.network.securitygroup.SecurityGroupSdnBackend;

public interface SdnControllerFactory {
    SdnControllerType getVendorType();

    default SdnControllerStatus resolveStatus(SdnControllerStatusEvent event, SdnControllerVO vo) {
        switch (event) {
            case RECONNECT_STARTED:   return SdnControllerStatus.Connecting;
            case RECONNECT_SUCCESS:   return SdnControllerStatus.Connected;
            case RECONNECT_FAILED:    return SdnControllerStatus.Disconnected;
            case PING_FAILED:         return SdnControllerStatus.Disconnected;
            case INIT_SYNC_STARTED:   return SdnControllerStatus.Syncing;
            case INIT_SYNC_SUCCESS:   return SdnControllerStatus.Connected;
            case INIT_SYNC_FAILED:    return SdnControllerStatus.Disconnected;
            default:                  return vo.getStatus();
        }
    }

    void changeSdnControllerStatus(SdnControllerVO vo, SdnControllerStatusEvent event);

    SdnController getSdnController(SdnControllerVO vo);

    default SdnController getSdnController(String l2NetworkUuid) {return null;};

    SdnControllerL2 getSdnControllerL2(SdnControllerVO vo);
    default SdnControllerL2 getSdnControllerL2(String l2NetworkUuid) {return null;};

    default SdnControllerL3 getSdnControllerL3(SdnControllerVO vo) {return null;};

    SecurityGroupSdnBackend getSdnControllerSecurityGroup(SdnControllerVO vo);


    default SdnControllerDhcp getSdnControllerDhcp(SdnControllerVO vo) {return null;};
    default SdnControllerDhcp getSdnControllerDhcp(String l2NetworkUuid) {return null;};

    default FlowChain getSyncChain() {return FlowChainBuilder.newSimpleFlowChain();};
}
