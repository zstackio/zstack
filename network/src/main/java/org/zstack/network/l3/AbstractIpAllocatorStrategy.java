package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.*;

import javax.persistence.TypedQuery;
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

    @Transactional
    protected UsedIpInventory allocateRequiredIp(IpAllocateMessage msg) {
        String sql = "select ipr from IpRangeVO ipr where ipr.startIp <= :ip and ipr.endIp >= :ip and ipr.l3NetworkUuid = :l3uuid";
        TypedQuery<IpRangeVO> q = dbf.getEntityManager().createQuery(sql, IpRangeVO.class);
        q.setParameter("ip", msg.getRequiredIp());
        q.setParameter("l3uuid", msg.getL3NetworkUuid());
        List<IpRangeVO> iprs = q.getResultList();

        if (iprs.isEmpty()) {
            throw new OperationFailureException(errf.instantiateErrorCode(L3Errors.ALLOCATE_IP_ERROR,
                    String.format("cannot find ip range that has ip[%s] in l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid())
            ));
        }

        if (iprs.size() > 1) {
            throw new OperationFailureException(errf.instantiateErrorCode(L3Errors.ALLOCATE_IP_ERROR,
                    String.format("find more than one ip range that has ip[%s] in l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid())
            ));
        }

        IpRangeVO ipr = iprs.get(0);
        return l3NwMgr.reserveIp(IpRangeInventory.valueOf(ipr), msg.getRequiredIp());
    }
}
