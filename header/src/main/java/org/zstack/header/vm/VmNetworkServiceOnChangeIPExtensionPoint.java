package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;

public interface VmNetworkServiceOnChangeIPExtensionPoint {
    void releaseNetworkServiceOnChangeIP(VmInstanceSpec spec, Completion completion);
    void applyNetworkServiceOnChangeIP(VmInstanceSpec spec, Completion completion);
}
