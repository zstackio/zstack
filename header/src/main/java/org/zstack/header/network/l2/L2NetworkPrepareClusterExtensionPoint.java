package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;

import java.util.Map;

public interface L2NetworkPrepareClusterExtensionPoint {
    void prepareAttach(L2NetworkInventory network, String clusterUuid, Completion completion);

    default void prepareAttach(L2NetworkInventory network, String clusterUuid, Map<String, Object> headers, Completion completion) {
        prepareAttach(network, clusterUuid, completion);
    }

    default void beforeCommitAttachInDb(L2NetworkInventory network, String clusterUuid, Map<String, Object> headers) {
    }

    default void afterAttachFinally(L2NetworkInventory network, String clusterUuid, Map<String, Object> headers) {
    }
}
