package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;

/**
 * Created by boce.wang on 07/01/2025.
 */
public interface AfterDeleteIpRangeExtensionPoint {
    void afterDeleteIpRange(IpRangeInventory ipr);
}
