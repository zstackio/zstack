package org.zstack.header.network.l2;

import org.zstack.header.network.NetworkOperationContext;
import org.zstack.header.core.Completion;

import java.util.Map;

public interface L2NetworkPrepareClusterExtensionPoint {
    void prepareAttach(L2NetworkInventory network, String clusterUuid, Completion completion);

    default void prepareAttach(L2NetworkInventory network, String clusterUuid, NetworkOperationContext operation, Completion completion) {
        prepareAttach(network, clusterUuid, completion);
    }

    default void beforeCommitAttachInDb(L2NetworkInventory network, String clusterUuid, NetworkOperationContext operation) {
    }

    default void afterAttachFinally(L2NetworkInventory network, String clusterUuid, NetworkOperationContext operation) {
    }
}
