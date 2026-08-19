package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;

/** Provider hook for confirmed remote-first L2 deletion. */
public interface L2DeleteConfirmExtensionPoint {
    boolean supports(L2NetworkInventory inventory);
    void begin(L2NetworkInventory inventory, NetworkDeletionContext context, Completion completion);
    void check(L2NetworkInventory inventory, NetworkDeletionContext context, Completion completion);
    void delete(L2NetworkInventory inventory, NetworkDeletionContext context, Completion completion);
    void cancel(L2NetworkInventory inventory, NetworkDeletionContext context, Completion completion);
    void deleteLocalMetadata(L2NetworkInventory inventory);
    default void deleteLocalMetadata(L2NetworkInventory inventory, NetworkDeletionContext context) {
        deleteLocalMetadata(inventory);
    }
}
