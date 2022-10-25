package org.zstack.sugonSdnController.controller;

import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.SdnControllerType;
import org.zstack.sdnController.header.SdnControllerVO;

public class SugonSdnControllerFactory implements SdnControllerFactory {

    SdnControllerType sdnControllerType = new SdnControllerType(SugonSdnControllerConstant.TF_CONTROLLER);
    @Override
    public SdnControllerType getVendorType() {
        return sdnControllerType;
    }

    @Override
    public SdnController getSdnController(SdnControllerVO vo) {
        return new SugonSdnController(vo);
    }
}
