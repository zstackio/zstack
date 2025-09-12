package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;

import java.util.List;

public interface SdnControllerL3 {
    void createL3Network(L3NetworkInventory inv, List<String> systemTags, Completion completion);
    void deleteL3Network(L3NetworkInventory inv, Completion completion);
    void createIpRange(IpRangeInventory inv, Completion completion);
    void deleteIpRange(IpRangeInventory inv, Completion completion);
}
