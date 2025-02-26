package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.network.l3.IpRangeInventory;

import java.util.List;

public interface SdnControllerDhcp {
    void addIpRange(List<IpRangeInventory> iprs, boolean sync, Completion completion);
    void removeIpRange(List<IpRangeInventory> iprs, Completion completion);
    void updateIpRange(List<IpRangeInventory> iprs, Completion completion);
}
