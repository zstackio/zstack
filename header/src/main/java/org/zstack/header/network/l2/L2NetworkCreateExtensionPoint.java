package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;
import org.zstack.header.network.NetworkException;

public interface L2NetworkCreateExtensionPoint {
    void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException;

    default void postCreateL2Network(L2NetworkInventory l2Network, APICreateL2NetworkMsg msg, Completion completion) {completion.success();}
    void afterCreateL2Network(L2NetworkInventory l2Network);
}
