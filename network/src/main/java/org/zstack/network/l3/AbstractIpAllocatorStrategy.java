package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

/**
 */
public abstract class AbstractIpAllocatorStrategy implements IpAllocatorStrategy  {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L3NetworkManager l3NwMgr;
    @Autowired
    protected ErrorFacade errf;

    protected UsedIpInventory allocateRequiredIp(IpAllocateMessage msg) {
        SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
        q.add(IpRangeVO_.l3NetworkUuid, SimpleQuery.Op.EQ, msg.getL3NetworkUuid());
        List<IpRangeVO> iprs = q.list();

        final long rip = NetworkUtils.ipv4StringToLong(msg.getRequiredIp());

        IpRangeVO ipr = CollectionUtils.find(iprs, new Function<IpRangeVO, IpRangeVO>() {
            @Override
            public IpRangeVO call(IpRangeVO arg) {
                long s = NetworkUtils.ipv4StringToLong(arg.getStartIp());
                long e = NetworkUtils.ipv4StringToLong(arg.getEndIp());
                return s <= rip && rip <= e ? arg : null;
            }
        });

        if (ipr == null) {
            throw new OperationFailureException(errf.instantiateErrorCode(L3Errors.ALLOCATE_IP_ERROR,
                    String.format("cannot find ip range that has ip[%s] in l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid())
            ));
        }

        return l3NwMgr.reserveIp(IpRangeInventory.valueOf(ipr), msg.getRequiredIp());
    }
}
