package org.zstack.header.network.l3;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l2.NetworkDeletionContext;

/**
 * Created by frank on 11/15/2015.
 */
public interface IpRangeDeletionExtensionPoint {
    void preDeleteIpRange(IpRangeInventory ipRange);
    default void preDeleteIpRange(IpRangeInventory ipRange, NetworkDeletionContext context) {
        preDeleteIpRange(ipRange);
    }

    void beforeDeleteIpRange(IpRangeInventory ipRange);
    default void beforeDeleteIpRange(IpRangeInventory ipRange, NetworkDeletionContext context) {
        beforeDeleteIpRange(ipRange);
    }

    void afterDeleteIpRange(IpRangeInventory ipRange);
    default void afterDeleteIpRange(IpRangeInventory ipRange, NetworkDeletionContext context) {
        afterDeleteIpRange(ipRange);
    }

    void failedToDeleteIpRange(IpRangeInventory ipRange, ErrorCode errorCode);
}
