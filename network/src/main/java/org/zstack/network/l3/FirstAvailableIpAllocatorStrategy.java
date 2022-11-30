package org.zstack.network.l3;

import org.apache.commons.math3.analysis.function.Add;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.network.l3.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FirstAvailableIpAllocatorStrategy extends AbstractIpAllocatorStrategy{
    private static final CLogger logger = Utils.getLogger(FirstAvailableIpAllocatorStrategy.class);
    private static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY);
    
    @Override
    public IpAllocatorType getType() {
        return type;
    }
    
    private String allocateIp(IpRangeVO vo, String excludeIp) {
        List<BigInteger> used = l3NwMgr.getUsedIpInRange(vo);
        used.add(new BigInteger(String.valueOf(NetworkUtils.ipv4StringToLong(excludeIp))));
        List<Long> usedIP = used.stream().map(BigInteger::longValue).collect(Collectors.toList());
        Collections.sort(used);
        return NetworkUtils.findFirstAvailableIpv4Address(vo.getStartIp(), vo.getEndIp(), usedIP.toArray(new Long[usedIP.size()]));
    }
    
    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        if (msg.getRequiredIp() != null) {
            return allocateRequiredIp(msg);
        }

        String excludeIp = msg.getExcludedIp();
        List<IpRangeVO> ranges;
        /* when allocate ip address from address pool, ipRangeUuid is not null  except for vip */
        if (msg.getIpRangeUuid() != null) {
            ranges = Q.New(IpRangeVO.class).eq(IpRangeVO_.uuid, msg.getIpRangeUuid()).list();
        } else {
            ranges = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
            if (msg.isUseAddressPoolIfNotRequiredIpRange())/* for vip */ {
                ranges.addAll(Q.New(AddressPoolVO.class).eq(AddressPoolVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .eq(AddressPoolVO_.ipVersion, IPv6Constants.IPv4).list());
            }
        }

        do {
            String ip = null;
            IpRangeVO tr = null;
            
            for (IpRangeVO r : ranges) {
                if (l3NwMgr.isIpRangeFull(r)) {
                    logger.debug(String.format("Ip range[uuid:%s, name: %s] is exhausted, try next one", r.getUuid(), r.getName()));
                    continue;
                }

                ip = allocateIp(r, excludeIp);
                tr = r;
                if (ip != null) {
                    break;
                }
            }
            
            if (ip == null) {
                /* No available ip in ranges */
                return null;
            }
            
            UsedIpInventory inv = l3NwMgr.reserveIp(tr, ip, msg.isDuplicatedIpAllowed());
            if (inv != null) {
                return inv;
            }
        } while (true);
    }
}
