package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.header.APIAddSdnControllerMsg;
import org.zstack.sdnController.header.SdnControllerVO;

public interface SdnControllerFactory {
    SdnControllerType getVendorType();

    void createSdnController(SdnControllerVO vo, APIAddSdnControllerMsg msg, Completion completion);

    SdnController getSdnController(SdnControllerVO vo);

    int getMappingVlanIdFromHardwareVxlanNetwork(L2VxlanNetworkInventory vxlan, String controllerUuid);
}
