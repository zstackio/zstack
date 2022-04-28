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
import org.zstack.utils.network.IPv6Constants;
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
        List<IpRangeVO> iprs;
        /* when allocate ip address from address pool, ipRangeUuid is not null  except for vip */
        if (msg.getIpRangeUuid() != null) {
            iprs = Q.New(IpRangeVO.class).eq(IpRangeVO_.uuid, msg.getIpRangeUuid()).list();
        } else {
            iprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
            if (msg.isUseAddressPoolIfNotRequiredIpRange()) /* for vip */ {
                iprs.addAll(Q.New(AddressPoolVO.class).eq(AddressPoolVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .eq(AddressPoolVO_.ipVersion, IPv6Constants.IPv4).list());
            }
        }
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

        return l3NwMgr.reserveIp(ipr, msg.getRequiredIp(), msg.isDuplicatedIpAllowed());
    }

    protected UsedIpInventory allocateRequiredIpv6(IpAllocateMessage msg) {
        List<IpRangeVO> iprs;
        /* when allocate ip address from address pool, ipRangeUuid is not null  except for vip */
        if (msg.getIpRangeUuid() != null) {
            iprs = Q.New(IpRangeVO.class).eq(IpRangeVO_.uuid, msg.getIpRangeUuid()).list();
        } else {
            iprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
            if (msg.isUseAddressPoolIfNotRequiredIpRange()) /* for vip */
            {
                iprs.addAll(Q.New(AddressPoolVO.class).eq(AddressPoolVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .eq(AddressPoolVO_.ipVersion, IPv6Constants.IPv6).list());
            }
        }

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

        return l3NwMgr.reserveIp(ipr, msg.getRequiredIp(), msg.isDuplicatedIpAllowed());
    }
}
