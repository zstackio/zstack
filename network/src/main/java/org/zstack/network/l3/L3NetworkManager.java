package org.zstack.network.l3;

import com.googlecode.ipv6.IPv6Address;
import org.zstack.header.network.l3.*;

import java.math.BigInteger;
import java.util.List;

public interface L3NetworkManager {
    IpAllocatorStrategy getIpAllocatorStrategy(IpAllocatorType type);
    
    UsedIpInventory reserveIp(IpRangeInventory ipRange, String ip);

    UsedIpInventory reserveIp(IpRangeInventory ipRange, IPv6Address ip);

    boolean isIpRangeFull(IpRangeVO vo);
    
    List<BigInteger> getUsedIpInRange(IpRangeVO vo);


    L3NetworkFactory getL3NetworkFactory(L3NetworkType type);

    void updateIpAllocationMsg(AllocateIpMsg msg, String mac);
}
