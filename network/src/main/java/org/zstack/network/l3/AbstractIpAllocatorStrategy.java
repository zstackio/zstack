package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

import static org.zstack.core.Platform.err;

/**
 */
public abstract class AbstractIpAllocatorStrategy implements IpAllocatorStrategy  {
    private static final CLogger logger = Utils.getLogger(AbstractIpAllocatorStrategy.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L3NetworkManager l3NwMgr;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

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
            L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
            if (!l3NetworkVO.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
                for (AfterAllocateRequiredIpExtensionPoint extp : pluginRgty.getExtensionList(AfterAllocateRequiredIpExtensionPoint.class)) {
                    ipr = extp.afterAllocateRequiredIp(msg, ipr, iprs);
                }
            }
        }

        if (ipr == null) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR,
                    "cannot find ip range that has ip[%s] in l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid()
            ));
        }

        return l3NwMgr.reserveIp(IpRangeInventory.valueOf(ipr), msg.getRequiredIp());
    }

    protected UsedIpInventory allocateRequiredIpv6(IpAllocateMessage msg) {
        SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
        q.add(IpRangeVO_.l3NetworkUuid, SimpleQuery.Op.EQ, msg.getL3NetworkUuid());
        List<IpRangeVO> iprs = q.list();

        IpRangeVO ipr = CollectionUtils.find(iprs, new Function<IpRangeVO, IpRangeVO>() {
            @Override
            public IpRangeVO call(IpRangeVO arg) {
                if (IPv6NetworkUtils.isIpv6InRange(msg.getRequiredIp(), arg.getStartIp(), arg.getEndIp())) {
                    return arg;
                } else {
                    return null;
                }
            }
        });

        if (ipr == null) {
            L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
            if (!l3NetworkVO.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
                for (AfterAllocateRequiredIpExtensionPoint extp : pluginRgty.getExtensionList(AfterAllocateRequiredIpExtensionPoint.class)) {
                    ipr = extp.afterAllocateRequiredIp(msg, ipr, iprs);
                }
            }
        }

        if (ipr == null) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR,
                    "cannot find ip range that has ip[%s] in l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid()
            ));
        }

        return l3NwMgr.reserveIp(IpRangeInventory.valueOf(ipr), msg.getRequiredIp());
    }
}
