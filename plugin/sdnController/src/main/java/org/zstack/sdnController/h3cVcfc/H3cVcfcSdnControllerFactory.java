package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.network.securitygroup.SecurityGroupSdnBackend;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.*;

public class H3cVcfcSdnControllerFactory implements SdnControllerFactory {
    SdnControllerType sdnControllerType = new SdnControllerType(SdnControllerConstant.H3C_VCFC_CONTROLLER);

    @Autowired
    DatabaseFacade dbf;

    @Override
    public SdnControllerType getVendorType() {
        return sdnControllerType;
    }


    @Override
    public SdnController getSdnController(SdnControllerVO vo) {
        if (SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V2.equals(vo.getVendorVersion())) {
            return new H3cVcfcV2SdnController(vo);
        } else {
            return new H3cVcfcSdnController(vo);
        }
    }

    @Override
    public SdnControllerL2 getSdnControllerL2(SdnControllerVO vo) {
         if (SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V2.equals(vo.getVendorVersion())) {
            return new H3cVcfcV2SdnController(vo);
        } else {
            return new H3cVcfcSdnController(vo);
        }
    }

    @Override
    public SecurityGroupSdnBackend getSdnControllerSecurityGroup(SdnControllerVO vo) {
        return null;
    }
}
