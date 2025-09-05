package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.network.securitygroup.SecurityGroupSdnBackend;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.SdnControllerL2;
import org.zstack.sdnController.SdnControllerType;
import org.zstack.sdnController.header.*;

public class H3cVcfcSdnControllerFactory implements SdnControllerFactory {
    SdnControllerType sdnControllerType = new SdnControllerType(SdnControllerConstant.H3C_VCFC_CONTROLLER);

    @Autowired
    DatabaseFacade dbf;

    @Override
    public SdnControllerType getVendorType() {
        return sdnControllerType;
    }

    @Override
    public SdnControllerVO persistSdnController(SdnControllerVO vo) {
        vo = dbf.persistAndRefresh(vo);
        return vo;
    }


    @Override
    public SdnController getSdnController(SdnControllerVO vo) {
        return new H3cVcfcSdnController(vo);
    }

    @Override
    public SdnControllerL2 getSdnControllerL2(SdnControllerVO vo) {
        return new H3cVcfcSdnController(vo);
    }

    @Override
    public SecurityGroupSdnBackend getSdnControllerSecurityGroup(SdnControllerVO vo) {
        return null;
    }
}
