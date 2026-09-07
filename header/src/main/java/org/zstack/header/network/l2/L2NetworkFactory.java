package org.zstack.header.network.l2;

import org.zstack.header.core.ReturnValueCompletion;

public interface L2NetworkFactory {
    L2NetworkType getType();

    void createL2Network(L2NetworkVO vo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion);

    default void createL2Network(L2NetworkVO vo, APICreateL2NetworkMsg msg, NetworkCreateContext context,
                                 ReturnValueCompletion<L2NetworkInventory> completion) {
        createL2Network(vo, msg, completion);
    }

    L2Network getL2Network(L2NetworkVO vo);
}
