package org.zstack.sdnController.hardwareVxlan;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import java.util.List;

public interface HardwareVxlanNetworkExtensionPoint {
    void createVxlanNetworkOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void attachL2NetworkToClusterOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void deleteVxlanNetworkOnSdnController(VxlanNetworkVO vo, Completion completion);
}
