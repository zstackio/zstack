package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;

public interface L2NetworkDeleteExtensionPoint {
    void preDeleteL2Network(L2NetworkInventory inventory) throws L2NetworkException;

    void beforeDeleteL2Network(L2NetworkInventory inventory);

    default void deleteL2Network(L2NetworkInventory inv, NoErrorCompletion completion) {completion.done();}

    default void deleteL2Network(L2NetworkInventory inv, Completion completion) {
        deleteL2Network(inv, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    void afterDeleteL2Network(L2NetworkInventory inventory);
}
