package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;
import org.zstack.header.network.NetworkException;

public interface L2NetworkCreateExtensionPoint {
    void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException;

    default void beforeCreateL2Network(APICreateL2NetworkMsg msg, NetworkCreateContext context) throws NetworkException {
        beforeCreateL2Network(msg);
    }

    default void postCreateL2Network(L2NetworkInventory l2Network, APICreateL2NetworkMsg msg, Completion completion) {completion.success();}
    default void postCreateL2Network(L2NetworkInventory l2Network, APICreateL2NetworkMsg msg,
                                     NetworkCreateContext context, Completion completion) {
        postCreateL2Network(l2Network, msg, completion);
    }
    void afterCreateL2Network(L2NetworkInventory l2Network);
}
