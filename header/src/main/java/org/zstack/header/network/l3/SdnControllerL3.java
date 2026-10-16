package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;

import java.util.List;
import java.util.Map;

public interface SdnControllerL3 {
    default boolean isRemoteWriteFirst() {
        return false;
    }

    void createL3Network(L3NetworkInventory inv, List<String> systemTags, Completion completion);
    default void createL3Network(L3NetworkInventory inv, List<String> systemTags,
                                 Map<String, Object> headers, Completion completion) {
        createL3Network(inv, systemTags, completion);
    }
    default void beforeCommitL3NetworkInDb(L3NetworkInventory l3, Map<String, Object> headers) {
    }
    default void beforeCommitL3NetworkInDb(L3NetworkInventory inv, List<String> systemTags,
                                           Map<String, Object> headers) {
        beforeCommitL3NetworkInDb(inv, headers);
    }
    default void afterCommitL3NetworkInDb(L3NetworkInventory inv, List<String> systemTags,
                                          Map<String, Object> headers) {
    }
    default void afterL3NetworkFinally(L3NetworkInventory l3, Map<String, Object> headers) {
    }
    default void afterL3NetworkFinally(L3NetworkInventory inv, List<String> systemTags,
                                       Map<String, Object> headers) {
        afterL3NetworkFinally(inv, headers);
    }
    void deleteL3Network(L3NetworkInventory inv, Completion completion);
    void createIpRange(IpRangeInventory inv, Completion completion);
    default void createIpRange(IpRangeInventory inv, Map<String, Object> headers, Completion completion) {
        createIpRange(inv, completion);
    }
    default void createIpRange(IpRangeInventory inv, List<String> systemTags,
                               Map<String, Object> headers, Completion completion) {
        createIpRange(inv, headers, completion);
    }
    default void beforeCommitIpRangeInDb(IpRangeInventory ipr, Map<String, Object> headers) {
    }
    default void afterCommitIpRangeInDb(IpRangeInventory ipr, Map<String, Object> headers, Completion completion) {
        completion.success();
    }
    default void afterCommitIpRangeInDb(IpRangeInventory inv, Map<String, Object> headers) {
    }
    default void afterIpRangeFinally(IpRangeInventory ipr, Map<String, Object> headers) {
    }
    void deleteIpRange(IpRangeInventory inv, Completion completion);

    default void deleteIpRange(IpRangeInventory inv, boolean lastIpRange,
                               Map<String, Object> headers,
                               Completion completion) {
        deleteIpRange(inv, lastIpRange, completion);
    }

    default void beforeDeleteIpRange(IpRangeInventory ipr, boolean lastIpRange,
                                     Map<String, Object> headers, Completion completion) {
        completion.success();
    }

    default void beforeCommitDeleteIpRangeInDb(IpRangeInventory ipr, Map<String, Object> headers) {
    }

    default void beforeCommitDeleteIpRangeInDb(IpRangeInventory inv, boolean lastIpRange,
                                               Map<String, Object> headers) {
        beforeCommitDeleteIpRangeInDb(inv, headers);
    }

    default void beforeChangeDns(L3NetworkInventory l3, String dns, boolean isAdd,
                                 Map<String, Object> headers, Completion completion) {
        completion.success();
    }

    default void beforeCommitDnsInDb(L3NetworkInventory l3, Map<String, Object> headers) {
    }

    default void afterChangeDnsFinally(L3NetworkInventory l3, Map<String, Object> headers) {
    }

    default void beforeChangeNetworkService(L3NetworkInventory l3, Map<String, List<String>> networkServices,
                                            boolean isEnable, Map<String, Object> headers, Completion completion) {
        completion.success();
    }

    default void beforeCommitNetworkServiceInDb(L3NetworkInventory l3, Map<String, Object> headers) {
    }

    default void afterChangeNetworkServiceFinally(L3NetworkInventory l3, Map<String, Object> headers) {
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
