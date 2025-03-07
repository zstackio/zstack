package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;

import java.util.List;

public interface IpRangeBackendExtensionPoint {
    void addIpRange(List<IpRangeInventory> ipr, Completion completion);
    void removeIpRange(List<IpRangeInventory> ipr, Completion completion);
}
