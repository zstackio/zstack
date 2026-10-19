package org.zstack.header.network.l3;

import org.zstack.header.network.NetworkOperationContext;
import org.zstack.header.core.Completion;

import java.util.List;
import java.util.Map;

public interface SdnControllerL3 {
    default boolean isRemoteWriteFirst() {
        return false;
    }

    void createL3Network(L3NetworkInventory inv, List<String> systemTags, Completion completion);
    default void createL3Network(L3NetworkInventory inv, List<String> systemTags,
                                 NetworkOperationContext operation, Completion completion) {
        createL3Network(inv, systemTags, completion);
    }
    default void beforeCommitL3NetworkInDb(L3NetworkInventory l3, NetworkOperationContext operation) {
    }
    default void beforeCommitL3NetworkInDb(L3NetworkInventory inv, List<String> systemTags,
                                           NetworkOperationContext operation) {
        beforeCommitL3NetworkInDb(inv, operation);
    }
    default void afterCommitL3NetworkInDb(L3NetworkInventory inv, List<String> systemTags,
                                          NetworkOperationContext operation) {
    }
    default void afterL3NetworkFinally(L3NetworkInventory l3, NetworkOperationContext operation) {
    }
    default void afterL3NetworkFinally(L3NetworkInventory inv, List<String> systemTags,
                                       NetworkOperationContext operation) {
        afterL3NetworkFinally(inv, operation);
    }
    void deleteL3Network(L3NetworkInventory inv, Completion completion);
    void createIpRange(IpRangeInventory inv, Completion completion);
    default void createIpRange(IpRangeInventory inv, NetworkOperationContext operation, Completion completion) {
        createIpRange(inv, completion);
    }
    default void createIpRange(IpRangeInventory inv, List<String> systemTags,
                               NetworkOperationContext operation, Completion completion) {
        createIpRange(inv, operation, completion);
    }
    default void beforeCommitIpRangeInDb(IpRangeInventory ipr, NetworkOperationContext operation) {
    }
    default void afterCommitIpRangeInDb(IpRangeInventory ipr, NetworkOperationContext operation, Completion completion) {
        completion.success();
    }
    default void afterCommitIpRangeInDb(IpRangeInventory inv, NetworkOperationContext operation) {
    }
    default void afterIpRangeFinally(IpRangeInventory ipr, NetworkOperationContext operation) {
    }
    void deleteIpRange(IpRangeInventory inv, Completion completion);

    default void deleteIpRange(IpRangeInventory inv, boolean lastIpRange,
                               NetworkOperationContext operation,
                               Completion completion) {
        deleteIpRange(inv, lastIpRange, completion);
    }

    default void beforeDeleteIpRange(IpRangeInventory ipr, boolean lastIpRange,
                                     NetworkOperationContext operation, Completion completion) {
        completion.success();
    }

    default void beforeCommitDeleteIpRangeInDb(IpRangeInventory ipr, NetworkOperationContext operation) {
    }

    default void beforeCommitDeleteIpRangeInDb(IpRangeInventory inv, boolean lastIpRange,
                                               NetworkOperationContext operation) {
        beforeCommitDeleteIpRangeInDb(inv, operation);
    }

    default void beforeChangeDns(L3NetworkInventory l3, String dns, boolean isAdd,
                                 NetworkOperationContext operation, Completion completion) {
        completion.success();
    }

    default void beforeCommitDnsInDb(L3NetworkInventory l3, NetworkOperationContext operation) {
    }

    default void afterChangeDnsFinally(L3NetworkInventory l3, NetworkOperationContext operation) {
    }

    default void beforeChangeNetworkService(L3NetworkInventory l3, Map<String, List<String>> networkServices,
                                            boolean isEnable, NetworkOperationContext operation, Completion completion) {
        completion.success();
    }

    default void beforeCommitNetworkServiceInDb(L3NetworkInventory l3, NetworkOperationContext operation) {
    }

    default void afterChangeNetworkServiceFinally(L3NetworkInventory l3, NetworkOperationContext operation) {
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
