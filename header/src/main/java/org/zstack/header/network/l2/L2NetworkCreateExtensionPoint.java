package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;
import org.zstack.header.network.NetworkException;

import java.util.Map;

public interface L2NetworkCreateExtensionPoint {
    void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException;

    default void postCreateL2Network(L2NetworkInventory l2Network, APICreateL2NetworkMsg msg, Completion completion) {completion.success();}
    default void postCreateL2Network(L2NetworkInventory l2Network, APICreateL2NetworkMsg msg,
                                     Map<String, Object> headers, Completion completion) {
        postCreateL2Network(l2Network, msg, completion);
    }
    default void beforeCommitL2NetworkInDb(L2NetworkInventory l2Network, APICreateL2NetworkMsg msg,
                                           Map<String, Object> headers) {
    }
    default void afterL2NetworkFinally(L2NetworkInventory l2Network, APICreateL2NetworkMsg msg,
                                       Map<String, Object> headers) {
    }
    void afterCreateL2Network(L2NetworkInventory l2Network);
}
