package org.zstack.header.network.l2;

import org.zstack.header.network.NetworkOperationContext;
import org.zstack.header.core.Completion;

import java.util.Collections;
import java.util.Map;

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

    default void beforeUpdateL2Network(L2NetworkInventory l2Inv, NetworkOperationContext operation, Completion completion) {
        completion.success();
    }

    default void beforeCommitUpdateL2NetworkInDb(L2NetworkInventory l2Inv, NetworkOperationContext operation) {
    }

    default void afterUpdateL2NetworkFinally(L2NetworkInventory l2Inv, NetworkOperationContext operation) {
    }

    default void beforeUpdateL2Network(L2NetworkInventory l2Inv, Completion completion) {
        beforeUpdateL2Network(l2Inv, new NetworkOperationContext(), completion);
    }
}
