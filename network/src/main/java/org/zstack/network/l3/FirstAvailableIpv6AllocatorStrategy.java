package org.zstack.network.l3;

import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.network.l3.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.math.BigInteger;
import java.util.List;

public class FirstAvailableIpv6AllocatorStrategy extends AbstractIpAllocatorStrategy{
    private static final CLogger logger = Utils.getLogger(FirstAvailableIpv6AllocatorStrategy.class);
    static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.FIRST_AVAILABLE_IPV6_ALLOCATOR_STRATEGY);
    
    @Override
    public IpAllocatorType getType() {
        return type;
    }

    private String allocateIp(IpRangeVO vo) {
        List<BigInteger> used = l3NwMgr.getUsedIpInRange(vo);
        BigInteger start = IPv6NetworkUtils.ipv6AddressToBigInteger(vo.getStartIp());
        BigInteger end = IPv6NetworkUtils.ipv6AddressToBigInteger(vo.getEndIp());

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

        SimpleQuery<IpRangeVO> query = dbf.createQuery(IpRangeVO.class);
        query.add(IpRangeVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        List<IpRangeVO> ranges = query.list();
        do {
            String ip = null;
            IpRangeVO tr = null;
            
            for (IpRangeVO r : ranges) {
                if (l3NwMgr.isIpRangeFull(r)) {
                    logger.debug(String.format("Ip range[uuid:%s, name: %s] is exhausted, try next one", r.getUuid(), r.getName()));
                    continue;
                }

                ip = allocateIp(r);
                tr = r;
                if (ip != null) {
                    break;
                }
            }
            
            if (ip == null) {
                /* No available ip in ranges */
                return null;
            }
            
            UsedIpInventory inv = l3NwMgr.reserveIp(IpRangeInventory.valueOf(tr), ip);
            if (inv != null) {
                return inv;
            }
        } while (true);
    }
}
