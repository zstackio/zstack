package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;

import java.util.List;

public interface SdnControllerDhcp {
    void allocateDhcpAndEnableDhcp(L3NetworkVO vo, List<String> systemTags, Completion completion);
    void enableDhcp(List<L3NetworkInventory> invs, boolean sync, Completion completion);
    void disableDhcp(List<L3NetworkInventory> invs, int ipversion, Completion completion);
}
