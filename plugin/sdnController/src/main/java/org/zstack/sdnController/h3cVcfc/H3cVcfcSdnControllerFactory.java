package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.SdnControllerType;
import org.zstack.sdnController.header.*;

import java.util.HashMap;
import java.util.Map;

import static org.zstack.utils.ObjectUtils.logger;

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
