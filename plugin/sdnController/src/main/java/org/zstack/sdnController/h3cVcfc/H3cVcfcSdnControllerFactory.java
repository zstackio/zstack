package org.zstack.sdnController.h3cVcfc;

import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.SdnControllerType;
import org.zstack.sdnController.header.*;

public class H3cVcfcSdnControllerFactory implements SdnControllerFactory {
    SdnControllerType sdnControllerType = new SdnControllerType(SdnControllerConstant.H3C_VCFC_CONTROLLER);

    @Override
    public SdnControllerType getVendorType() {
        return sdnControllerType;
    }

    @Override
    public SdnController getSdnController(SdnControllerVO vo) {
        return new H3cVcfcSdnController(vo);
    }
}
