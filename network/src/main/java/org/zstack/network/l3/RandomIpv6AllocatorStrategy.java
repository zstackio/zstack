package org.zstack.network.l3;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.*;

public class RandomIpv6AllocatorStrategy extends AbstractIpAllocatorStrategy {
    private static final CLogger logger = Utils.getLogger(RandomIpv6AllocatorStrategy.class);
    public static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.RANDOM_IPV6_ALLOCATOR_STRATEGY);
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public IpAllocatorType getType() {
        return type;
    }

    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        if (msg.getRequiredIp() != null) {
            return allocateRequiredIpv6(msg);
        }

        SimpleQuery<IpRangeVO> query = dbf.createQuery(IpRangeVO.class);
        query.add(IpRangeVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        List<IpRangeVO> ranges = query.list();

        Collections.shuffle(ranges);

        do {
            IPv6Address ip = null;
            IpRangeVO tr = null;

            for (IpRangeVO r : ranges) {
                ip = allocateIp(r);
                tr = r;
                if (ip != null) {
                    break;
                }
            }

            if (ip == null) {

                return null;
            }

            UsedIpInventory inv = l3NwMgr.reserveIp(IpRangeInventory.valueOf(tr), ip);
            if (inv != null) {
                return inv;
            }
        } while (true);
    }


    private IPv6Address allocateIp(IpRangeVO vo) {
        BigInteger start = IPv6Address.fromString(vo.getStartIp()).toBigInteger();
        BigInteger end = IPv6Address.fromString(vo.getEndIp()).toBigInteger();

        long cnt = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipRangeUuid, vo.getUuid()).count();
        BigInteger num = start.add(new BigInteger(String.valueOf(cnt)));
        if (num.compareTo(end) > 0) {
            return null;
        }

        List<UsedIpVO> ips = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipRangeUuid, vo.getUuid()).list();
        Set<BigInteger> usedIps = new HashSet<>();
        for (UsedIpVO ip : ips) {
            BigInteger address = IPv6Address.fromString(ip.getIp()).toBigInteger();
            usedIps.add(address);
        }

        Random rnd = new Random();
        /* a stateful dhcp range with 2^24 is big enough */
        int total = end.subtract(start).intValue();
        if ((total > (1 << 23)) || (total < 0)) {
            total = (1 << 23);
        }
        IPv6Address address = null;
        do {
            num = start.add(new BigInteger(String.valueOf(rnd.nextInt(total + 1))));
            if (!usedIps.contains(num)) {
                address = IPv6Address.fromBigInteger(num);
                break;
            }
        } while (true);

        return  address;
    }
}
