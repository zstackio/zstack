package org.zstack.sdnController.hardwareVxlan;

import org.zstack.header.core.Completion;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolVO;

public interface HardwareVxlanNetworkPoolExtensionPoint {
    void preCreateVxlanNetworkPoolOnSdnController(HardwareL2VxlanNetworkPoolVO vo, Completion completion);
}
