package org.zstack.sdnController;

import org.zstack.sdnController.header.SdnControllerVO;

public interface SdnControllerFactory {
    SdnControllerType getVendorType();

    SdnController getSdnController(SdnControllerVO vo);
}
