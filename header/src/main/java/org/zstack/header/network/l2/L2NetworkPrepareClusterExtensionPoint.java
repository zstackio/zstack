package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;

public interface L2NetworkPrepareClusterExtensionPoint {
    default void prepareAttach(L2NetworkInventory network, AttachL2NetworkToClusterMsg message,
                               Completion completion) {
        prepareAttach(network, message.getClusterUuid(), message.getOrigin(), completion);
    }

    default void prepareDetach(L2NetworkInventory network, DetachL2NetworkFromClusterMsg message,
                               Completion completion) {
        prepareDetach(network, message.getClusterUuid(), message.getOrigin(), completion);
    }

    void prepareAttach(L2NetworkInventory network, String clusterUuid, Completion completion);

    default void prepareAttach(L2NetworkInventory network, String clusterUuid,
                               NetworkOperationOrigin origin, Completion completion) {
        prepareAttach(network, clusterUuid, completion);
    }

    default void prepareDetach(L2NetworkInventory network, String clusterUuid,
                               NetworkOperationOrigin origin, Completion completion) {
        completion.success();
    }
}
