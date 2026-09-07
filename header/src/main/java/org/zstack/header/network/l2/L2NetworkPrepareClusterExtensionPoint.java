package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;

public interface L2NetworkPrepareClusterExtensionPoint {
    void prepareAttach(L2NetworkInventory network, String clusterUuid, Completion completion);
}
