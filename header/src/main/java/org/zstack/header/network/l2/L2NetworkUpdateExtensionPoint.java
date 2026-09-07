package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;

/**
 * Created by boce.wang on 03/25/2024.
 */
public interface L2NetworkUpdateExtensionPoint {

    void beforeChangeL2NetworkVlanId(L2NetworkInventory l2Inv);

    default void beforeChangeL2NetworkVlanId(L2NetworkInventory l2Inv, Completion completion) {
        beforeChangeL2NetworkVlanId(l2Inv);
        completion.success();
    }

    void afterChangeL2NetworkVlanId(L2NetworkInventory l2Inv);
}
