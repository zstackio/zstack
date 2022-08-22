package org.zstack.sdnController.h3c;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.SdnControllerType;
import org.zstack.sdnController.header.APIAddSdnControllerMsg;
import org.zstack.sdnController.header.SdnControllerConstant;
import org.zstack.sdnController.header.SdnControllerVO;

public class H3cSdnControllerFactory implements SdnControllerFactory {
    SdnControllerType sdnControllerType = new SdnControllerType(SdnControllerConstant.H3C_VCFC_CONTROLLER);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SdnControllerType getVendorType() {
        return sdnControllerType;
    }

    @Override
    public void createSdnController(final SdnControllerVO vo, APIAddSdnControllerMsg msg, Completion completion) {
        SdnControllerVO sdnControllerVO = dbf.persistAndRefresh(vo);

        H3cSdnController controller = new H3cSdnController(sdnControllerVO);
        controller.postInitSdnController(msg, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.removeByPrimaryKey(vo.getUuid(), SdnControllerVO.class);
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public SdnController getSdnController(SdnControllerVO vo) {
        return new H3cSdnController(vo);
    }

    @Override
    public int getMappingVlanIdFromHardwareVxlanNetwork(L2VxlanNetworkInventory vxlan, String controllerUuid) {
        return 0;
    }
}
