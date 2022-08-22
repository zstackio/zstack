package org.zstack.sdnController.hardwareVxlan;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import java.util.Map;

public interface HardwareVxlanNetworkExtensionPoint {
    void postCreateVxlanNetworkOnSdnController(VxlanNetworkVO vo, Completion completion);
    void postDeleteVxlanNetworkOnSdnController(VxlanNetworkVO vo, Completion completion);
    void attachHardwareVxlanToCluster(VxlanNetworkVO vo, Completion completion);
    int getMappingVxlanId(String hostUuid);
    Map<Integer, String> getMappingVlanIdAndPhysicalInterface(L2VxlanNetworkInventory vxlan, String hostUuid);
}
