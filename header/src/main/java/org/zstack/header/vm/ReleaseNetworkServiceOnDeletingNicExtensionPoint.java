package org.zstack.header.vm;

import org.zstack.header.core.Completion;

public interface ReleaseNetworkServiceOnDeletingNicExtensionPoint {
    void releaseNetworkServiceOnDeletingNic(VmNicInventory nic, Completion completion);
}
