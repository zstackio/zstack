package org.zstack.sdnController.hardwareVxlan;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import java.util.List;
import java.util.Map;

public interface HardwareVxlanNetworkExtensionPoint {
    void preCreateVxlanNetworkOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void createVxlanNetworkOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void postCreateVxlanNetworkOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void preAttachL2NetworkToClusterOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void attachL2NetworkToClusterOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void postAttachL2NetworkToClusterOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void deleteVxlanNetworkOnSdnController(VxlanNetworkVO vo, Completion completion);
    Integer getMappingVxlanId(String hostUuid);
    Map<Integer, String> getMappingVlanIdAndPhysicalInterface(L2VxlanNetworkInventory vxlan, String hostUuid);
}
