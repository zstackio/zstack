package org.zstack.sdnController;

import org.zstack.sdnController.header.SdnControllerVO;

public interface SdnControllerManager {
    SdnControllerFactory getSdnControllerFactory(String type);
    SdnController getSdnController(SdnControllerVO sdnControllerVO);

}
