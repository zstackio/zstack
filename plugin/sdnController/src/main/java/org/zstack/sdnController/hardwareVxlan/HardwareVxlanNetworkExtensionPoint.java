package org.zstack.sdnController.hardwareVxlan;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

public interface HardwareVxlanNetworkExtensionPoint {
    void createVxlanNetworkOnSdnController(VxlanNetworkVO vo, Completion completion);
    void deleteVxlanNetworkOnSdnController(VxlanNetworkVO vo, Completion completion);
    void attachHardwareVxlanToCluster(VxlanNetworkVO vo, Completion completion);
    int getMappingVxlanId(String hostUuid);
}
