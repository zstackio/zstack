package org.zstack.network.l3;

import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.network.l3.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

public class FirstAvailableIpAllocatorStrategy extends AbstractIpAllocatorStrategy{
    private static final CLogger logger = Utils.getLogger(FirstAvailableIpAllocatorStrategy.class);
    static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY);
    
    @Override
    public IpAllocatorType getType() {
        return type;
    }
    
    private String allocateIp(IpRangeVO vo) {
        List<Long> used = l3NwMgr.getUsedIpInRange(vo.getUuid());
        String ret = NetworkUtils.findFirstAvailableIpv4Address(vo.getStartIp(), vo.getEndIp(), used.toArray(new Long[used.size()]));
        return ret;
    }
    
    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        if (msg.getRequiredIp() != null) {
            return allocateRequiredIp(msg);
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
