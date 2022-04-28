package org.zstack.network.l3;

import org.zstack.core.db.Q;
import org.zstack.header.network.l3.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class FirstAvailableIpv6AllocatorStrategy extends AbstractIpAllocatorStrategy{
    private static final CLogger logger = Utils.getLogger(FirstAvailableIpv6AllocatorStrategy.class);
    private static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.FIRST_AVAILABLE_IPV6_ALLOCATOR_STRATEGY);
    
    @Override
    public IpAllocatorType getType() {
        return type;
    }

    private String allocateIp(IpRangeVO vo, String excludeIp) {
        List<BigInteger> used = l3NwMgr.getUsedIpInRange(vo);
        used.add(new BigInteger(String.valueOf(IPv6NetworkUtils.ipv6AddressToBigInteger(excludeIp))));
        BigInteger start = IPv6NetworkUtils.ipv6AddressToBigInteger(vo.getStartIp());
        BigInteger end = IPv6NetworkUtils.ipv6AddressToBigInteger(vo.getEndIp());
        Collections.sort(used);
        BigInteger target = IPv6NetworkUtils.findFirstAvailableIpv6Address(start,end, used.toArray(new BigInteger[used.size()]));
        if (target != null) {
            return IPv6NetworkUtils.ipv6AddressToString(target);
        }

        return null;
    }
    
    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        if (msg.getRequiredIp() != null) {
            return allocateRequiredIpv6(msg);
        }

        String excludeIp = msg.getExcludedIp();
        List<IpRangeVO> ranges;
        /* when allocate ip address from address pool, ipRangeUuid is not null  except for vip */
        if (msg.getIpRangeUuid() != null) {
            ranges = Q.New(IpRangeVO.class).eq(IpRangeVO_.uuid, msg.getIpRangeUuid()).list();
        } else {
            ranges = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
            if (msg.isUseAddressPoolIfNotRequiredIpRange()) /* for vip */
            {
                ranges.addAll(Q.New(AddressPoolVO.class).eq(AddressPoolVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .eq(AddressPoolVO_.ipVersion, IPv6Constants.IPv6).list());
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
