package org.zstack.header.network.l3;

public interface AfterUpdateIpRangeExtensionPoint {
    void afterUpdateIpRange(IpRangeInventory oldIpRange, IpRangeInventory newIpRange);
}
