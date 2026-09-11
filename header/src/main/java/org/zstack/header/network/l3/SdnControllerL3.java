package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.NetworkCreateContext;
import org.zstack.header.network.l2.NetworkDeletionContext;

import java.util.List;

public interface SdnControllerL3 {
    void createL3Network(L3NetworkInventory inv, List<String> systemTags, Completion completion);
    default void createL3Network(L3NetworkInventory inv, List<String> systemTags, NetworkCreateContext context, Completion completion) {
        createL3Network(inv, systemTags, completion);
    }
    void deleteL3Network(L3NetworkInventory inv, Completion completion);
    default void deleteL3Network(L3NetworkInventory inv, NetworkDeletionContext context,
                                 Completion completion) {
        deleteL3Network(inv, completion);
    }
    void createIpRange(IpRangeInventory inv, Completion completion);
    default void createIpRange(IpRangeInventory inv, NetworkCreateContext context, Completion completion) {
        createIpRange(inv, completion);
    }
    void deleteIpRange(IpRangeInventory inv, Completion completion);

    default void deleteIpRange(IpRangeInventory inv, boolean lastIpRange,
                               NetworkDeletionContext context, Completion completion) {
        deleteIpRange(inv, lastIpRange, completion);
    }

    default void deleteIpRange(IpRangeInventory inv, boolean lastIpRange, Completion completion) {
        if (lastIpRange) {
            deleteIpRange(inv, new Completion(completion) {
                @Override
                public void success() {
                    completion.success();
                }

                @Override
                public void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                    completion.success();
                }
            });
        } else {
            completion.success();
        }
    }
}
