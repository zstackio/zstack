package org.zstack.network.l3;

import org.zstack.header.network.l3.*;

import java.util.List;

public interface L3NetworkManager {
    IpAllocatorStrategy getIpAllocatorStrategy(IpAllocatorType type);
    
    UsedIpInventory reserveIp(IpRangeInventory ipRange, String ip);

    boolean isIpRangeFull(IpRangeVO vo);
    
    List<Long> getUsedIpInRange(String ipRangeUuid);


    L3NetworkFactory getL3NetworkFactory(L3NetworkType type);
}
