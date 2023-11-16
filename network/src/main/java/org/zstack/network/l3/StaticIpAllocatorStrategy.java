package org.zstack.network.l3;

import static org.zstack.core.Platform.err;

import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.AllocateIpMsg;
import org.zstack.header.network.l3.IpAllocateMessage;
import org.zstack.header.network.l3.IpAllocatorStrategy;
import org.zstack.header.network.l3.IpAllocatorType;
import org.zstack.header.network.l3.L3Errors;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;


public class StaticIpAllocatorStrategy implements IpAllocatorStrategy {
    private static final CLogger logger = Utils.getLogger(StaticIpAllocatorStrategy.class);

    public static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.STATIC_IP_ALLOCATOR_STRATEGY);

    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public IpAllocatorType getType() {
        return type;
    }

    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        if (!(msg instanceof AllocateIpMsg)) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "message[%s] is not AllocateIpMsg", msg.getClass()));
        }

        AllocateIpMsg amsg = (AllocateIpMsg) msg;

        L3NetworkVO l3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, amsg.getL3NetworkUuid()).find();
        if (l3.getEnableIPAM()) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "l3Network[uuid:%s] is using IPAM, cannot allocate static ip", amsg.getL3NetworkUuid()));
        }

        if (amsg.getRequiredIp() == null || amsg.getNetmask() == null) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "requiredIp and netmask cannot be null"));
        }

        if (!NetworkUtils.isIpAddress(amsg.getRequiredIp())) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "invalid requiredIp[%s]", amsg.getRequiredIp()));
        }

        if (NetworkUtils.isIpv4Address(amsg.getRequiredIp())) {
            if (!NetworkUtils.isNetmask(amsg.getNetmask())) {
                throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "invalid netmask[%s]", amsg.getNetmask()));
            }

            return allocateRequiredIp(amsg.getL3NetworkUuid(), amsg.getRequiredIp(), amsg.getNetmask(), IPv6Constants.IPv4);
        } else if (IPv6NetworkUtils.isIpv6Address(amsg.getRequiredIp())) {
            if (amsg.getPrefixLength() < 64 || amsg.getPrefixLength() > 128) {
                throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "invalid prefixLength[%s]", amsg.getPrefixLength()));
            }

            String ip6Netmask = IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(String.format("%s/%d", amsg.getRequiredIp(), amsg.getPrefixLength()));
            return allocateRequiredIp(amsg.getL3NetworkUuid(), amsg.getRequiredIp(), ip6Netmask, IPv6Constants.IPv6);
        } else {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "invalid requiredIp[%s]", amsg.getRequiredIp()));
        }
    }

    private UsedIpInventory allocateRequiredIp(String l3Uuid, String ip, String netmask, int ipVersion) {

        UsedIpVO conflictIp = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3Uuid).eq(UsedIpVO_.ipVersion, ipVersion).eq(UsedIpVO_.ip, ip).find();
        if (conflictIp != null) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "ip[%s] has been occupied by other usedIp[uuid:%s]", ip, conflictIp.getUuid()));
        }

        UsedIpVO vo = new UsedIpVO();
        vo.setUuid(Platform.getUuid());
        vo.setIp(ip);
        vo.setIpInLong(ipVersion == IPv6Constants.IPv4 ? NetworkUtils.ipv4StringToLong(ip) : IPv6NetworkUtils.ipv6AddressToBigInteger(ip).longValue());
        vo.setIpVersion(ipVersion);
        vo.setL3NetworkUuid(l3Uuid);
        vo.setNetmask(netmask);

        vo = dbf.persistAndRefresh(vo);

        return UsedIpInventory.valueOf(vo);
    }
}
