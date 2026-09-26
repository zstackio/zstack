package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.NetworkDeletionContext;

public interface L3NetworkDeleteExtensionPoint {
    String preDeleteL3Network(L3NetworkInventory inventory) throws L3NetworkException;

    default String preDeleteL3Network(L3NetworkInventory inventory, NetworkDeletionContext context)
            throws L3NetworkException {
        return preDeleteL3Network(inventory);
    }

    /**
     * Asynchronous, failable preparation that runs before the destructive
     * cascade starts. Backends use it to confirm remote state (for example
     * freezing a deletion intent); a failure here aborts the deletion before
     * anything is destroyed.
     */
    default void prepareDeleteL3Network(L3NetworkInventory inventory, NetworkDeletionContext context,
                                        Completion completion) {
        completion.success();
    }

    void beforeDeleteL3Network(L3NetworkInventory inventory);
    default void beforeDeleteL3Network(L3NetworkInventory inventory, NetworkDeletionContext context) {
        beforeDeleteL3Network(inventory);
    }

    void afterDeleteL3Network(L3NetworkInventory inventory);
    default void afterDeleteL3Network(L3NetworkInventory inventory, NetworkDeletionContext context) {
        afterDeleteL3Network(inventory);
    }
}
