package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.NetworkDeletionContext;

/**
 * Created by boce.wang on 07/01/2025.
 */
public interface AfterDeleteIpRangeExtensionPoint {
    void afterDeleteIpRange(IpRangeInventory ipr);
    default void afterDeleteIpRange(IpRangeInventory ipr, NetworkDeletionContext context) {
        afterDeleteIpRange(ipr);
    }
}
