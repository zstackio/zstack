package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceExtensionPoint.NetworkServiceExtensionPosition;
import org.zstack.header.vm.*;

public interface VmNetworkServiceOnChangeIPExtensionPoint {
    void releaseNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPosition position, Completion completion);
    void applyNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPosition position, Completion completion);
}
