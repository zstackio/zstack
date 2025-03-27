package org.zstack.sdnController;

import org.zstack.header.core.workflow.FlowChain;
import org.zstack.sdnController.header.SdnControllerVO;

public interface SdnControllerManager {
    SdnControllerFactory getSdnControllerFactory(String type);
    SdnController getSdnController(SdnControllerVO sdnControllerVO);
    SdnControllerL2 getSdnControllerL2(SdnControllerVO sdnControllerVO);
    FlowChain getSyncChain(SdnControllerVO sdnControllerVO);
}
