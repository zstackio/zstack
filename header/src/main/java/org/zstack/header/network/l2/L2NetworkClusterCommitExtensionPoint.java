package org.zstack.header.network.l2;

/** Commit checks run in the binding transaction; completion runs after the child message. */
public interface L2NetworkClusterCommitExtensionPoint {
    default void beforeAttachClusterCommit(AttachL2NetworkToClusterMsg msg) {
    }

    default void beforeDetachClusterCommit(DetachL2NetworkFromClusterMsg msg) {
    }
    default void afterClusterChange(String l2Uuid, String operationUuid,
                                    org.zstack.header.core.Completion completion) {
        completion.success();
    }
}
