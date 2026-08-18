package org.zstack.network.l3;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.SdnControllerDhcp;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;

import java.math.BigInteger;
import java.util.List;

public interface L3NetworkManager {
    IpAllocatorStrategy getIpAllocatorStrategy(IpAllocatorType type);

    UsedIpInventory reserveIp(IpRangeVO ipRange, String ip);
    UsedIpInventory reserveIp(IpRangeVO ipRange, String ip, boolean allowDuplicatedAddress);
    UsedIpInventory reserveIp(IpRangeVO ipRange, String ip, boolean allowDuplicatedAddress,
                              String operationUuid, String operationStep);

    boolean isIpRangeFull(IpRangeVO vo);
    
    List<BigInteger> getUsedIpInRange(IpRangeVO vo);


    L3NetworkFactory getL3NetworkFactory(L3NetworkType type);

    void updateIpAllocationMsg(AllocateIpMsg msg, String mac);

    void reAllocateNicIp(VmNicVO nicVO, ReturnValueCompletion<List<UsedIpInventory>> completion);

    IpRangeFactory getIpRangeFactory(IpRangeType type);

    List<VmNicInventory> filterVmNicByIpVersion(List<VmNicInventory> vmNics, int ipVersion);

    boolean applyNetworkServiceWhenVmStateChange(String type);

    SdnControllerDhcp getSdnControllerDhcp(String l3Uuid);

    /**
     * Resolve SDN controller L3 service bound to the given L2 network.
     *
     * @param l2Uuid L2Network UUID used to locate the SDN controller
     * @return SdnControllerL3 implementation, or null if none is available
     */
    SdnControllerL3 getSdnControllerL3(String l2Uuid);
}
