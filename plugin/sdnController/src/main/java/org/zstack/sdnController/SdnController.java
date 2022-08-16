package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.header.APIAddSdnControllerMsg;

public interface SdnController {
    void initSdnController(APIAddSdnControllerMsg msg, Completion completion);
    void createVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
    void deleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
    int  getMappingVlanId(L2VxlanNetworkInventory vxlan, String hostUuid);
}
