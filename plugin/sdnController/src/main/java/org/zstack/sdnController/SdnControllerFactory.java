package org.zstack.sdnController;

import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.sdnController.header.SdnControllerVO;

public interface SdnControllerFactory {
    SdnControllerType getVendorType();

    SdnController getSdnController(SdnControllerVO vo);

    default SdnController getSdnController(String l2NetworkUuid) {return null;};

    SdnControllerL2 getSdnControllerL2(SdnControllerVO vo);

    default SdnControllerL2 getSdnControllerL2(String l2NetworkUuid) {return null;};

    default SdnControllerDhcp getSdnControllerDhcp(SdnControllerVO vo) {return null;};

    default SdnControllerDhcp getSdnControllerDhcp(String l2NetworkUuid) {return null;};

    default FlowChain getSyncChain() {return FlowChainBuilder.newSimpleFlowChain();};
}
