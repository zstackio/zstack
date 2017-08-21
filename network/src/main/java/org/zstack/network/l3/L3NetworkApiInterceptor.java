package org.zstack.network.l3;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.commons.validator.routines.DomainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class L3NetworkApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof IpRangeMessage) {
            IpRangeMessage dmsg = (IpRangeMessage)msg;
            SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
            q.select(IpRangeVO_.l3NetworkUuid);
            q.add(IpRangeVO_.uuid, SimpleQuery.Op.EQ, dmsg.getIpRangeUuid());
            String l3NwUuid = q.findValue();
            dmsg.setL3NetworkUuid(l3NwUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3NwUuid);
        } else if (msg instanceof L3NetworkMessage) {
            L3NetworkMessage l3msg = (L3NetworkMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3msg.getL3NetworkUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddDnsToL3NetworkMsg) {
            validate((APIAddDnsToL3NetworkMsg) msg);
        } else if (msg instanceof APIAddIpRangeMsg) {
            validate((APIAddIpRangeMsg) msg);
        } else if (msg instanceof APIDeleteL3NetworkMsg) {
            validate((APIDeleteL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveDnsFromL3NetworkMsg) {
            validate((APIRemoveDnsFromL3NetworkMsg) msg);
        } else if (msg instanceof APICreateL3NetworkMsg) {
            validate((APICreateL3NetworkMsg) msg);
        } else if (msg instanceof APIGetIpAddressCapacityMsg) {
            validate((APIGetIpAddressCapacityMsg) msg);
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            validate((APIAddIpRangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIGetFreeIpMsg) {
            validate((APIGetFreeIpMsg) msg);
        } else if (msg instanceof APICheckIpAvailabilityMsg) {
            validate((APICheckIpAvailabilityMsg) msg);
        } else if (msg instanceof APISetL3NetworkRouterInterfaceIpMsg) {
            validate((APISetL3NetworkRouterInterfaceIpMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APISetL3NetworkRouterInterfaceIpMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getRouterInterfaceIp())) {
            throw new ApiMessageInterceptionException(argerr("invalid IP[%s]", msg.getRouterInterfaceIp()));
        }
        List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid()).list();
        if (ipRangeVOS == null || ipRangeVOS.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("no ip range in l3[%s]", msg.getL3NetworkUuid()));
        }
        for (IpRangeVO ipRangeVO : ipRangeVOS) {
            if (!NetworkUtils.isIpv4InCidr(msg.getRouterInterfaceIp(), ipRangeVO.getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("ip[%s] is not in the cidr of ip range[uuid:%s, cidr:%s] which l3 network[%s] attached",
                        msg.getRouterInterfaceIp(), ipRangeVO.getUuid(), ipRangeVO.getNetworkCidr(), msg.getL3NetworkUuid()));
            }
            if (NetworkUtils.isIpv4InRange(msg.getRouterInterfaceIp(), ipRangeVO.getStartIp(), ipRangeVO.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("ip[%s] in ip range[uuid:%s, startIp:%s, endIp:%s] which l3 network[%s] attached, this is not allowed",
                        msg.getRouterInterfaceIp(), ipRangeVO.getUuid(), ipRangeVO.getStartIp(), ipRangeVO.getEndIp(), msg.getL3NetworkUuid()));
            }
        }
    }

    private void validate(APICheckIpAvailabilityMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getIp())) {
            throw new ApiMessageInterceptionException(argerr("invalid IP[%s]", msg.getIp()));
        }
    }

    private void validate(APIGetFreeIpMsg msg) {
        if (msg.getIpRangeUuid() == null && msg.getL3NetworkUuid() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "ipRangeUuid and l3NetworkUuid cannot both be null; you must set either one."
            ));
        }

        if (msg.getIpRangeUuid() != null && msg.getL3NetworkUuid() == null) {
            SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
            q.select(IpRangeVO_.l3NetworkUuid);
            q.add(IpRangeVO_.uuid, Op.EQ, msg.getIpRangeUuid());
            String l3Uuid = q.findValue();
            msg.setL3NetworkUuid(l3Uuid);
        }

        if (msg.getLimit() < 0) {
            msg.setLimit(Integer.MAX_VALUE);
        }
    }

    private void validate(APIAddIpRangeByNetworkCidrMsg msg) {
        try {
            SubnetUtils utils = new SubnetUtils(msg.getNetworkCidr());
            utils.setInclusiveHostCount(false);
            SubnetInfo subnet = utils.getInfo();
            if (subnet.getAddressCount() == 0) {
                throw new ApiMessageInterceptionException(argerr("%s is not an allowed network cidr, because it doesn't have usable ip range", msg.getNetworkCidr()));
            }
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid network cidr", msg.getNetworkCidr()));
        }

        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validate(ipr);
    }

    private void validate(APIGetIpAddressCapacityMsg msg) {
        boolean pass = false;
        if (msg.getIpRangeUuids() != null && !msg.getIpRangeUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getL3NetworkUuids() != null && !msg.getL3NetworkUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
            pass = true;
        }

        if (!pass && !msg.isAll()) {
            throw new ApiMessageInterceptionException(argerr(
                    "ipRangeUuids, L3NetworkUuids, zoneUuids must have at least one be none-empty list, or all is set to true"
            ));
        }

        if (msg.isAll() && (msg.getZoneUuids() == null || msg.getZoneUuids().isEmpty())) {
            SimpleQuery<ZoneVO> q = dbf.createQuery(ZoneVO.class);
            q.select(ZoneVO_.uuid);
            List<String> zuuids = q.listValue();
            msg.setZoneUuids(zuuids);

            if (msg.getZoneUuids().isEmpty()) {
                APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }
    }

    private void validate(APICreateL3NetworkMsg msg) {
        if (!L3NetworkType.hasType(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported l3network type[%s]", msg.getType()));
        }

        if (msg.getDnsDomain() != null) {
            DomainValidator validator = DomainValidator.getInstance();
            if (!validator.isValid(msg.getDnsDomain())) {
                throw new ApiMessageInterceptionException(argerr("%s is not a valid domain name", msg.getDnsDomain()));
            }
        }
    }

    private void validate(APIRemoveDnsFromL3NetworkMsg msg) {
        SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
        q.add(L3NetworkDnsVO_.dns, Op.EQ, msg.getDns());
        q.add(L3NetworkDnsVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        if (!q.isExists()) {
            APIRemoveDnsFromL3NetworkEvent evt = new APIRemoveDnsFromL3NetworkEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIDeleteL3NetworkMsg msg) {
        if (!dbf.isExist(msg.getUuid(), L3NetworkVO.class)) {
            APIDeleteL3NetworkEvent evt = new APIDeleteL3NetworkEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(IpRangeInventory ipr) {
        if (NetworkUtils.isIpv4RangeOverlap("224.0.0.0", "239.255.255.255", ipr.getStartIp(), ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("the IP range[%s ~ %s] contains D class addresses which are for multicast", ipr.getStartIp(), ipr.getEndIp()));
        }

        if (NetworkUtils.isIpv4RangeOverlap("240.0.0.0", "255.255.255.255", ipr.getStartIp(), ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("the IP range[%s ~ %s] contains E class addresses which are reserved", ipr.getStartIp(), ipr.getEndIp()));
        }

        if (NetworkUtils.isIpv4RangeOverlap("169.254.1.0", "169.254.254.255", ipr.getStartIp(), ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("the IP range[%s ~ %s] contains link local addresses which are reserved", ipr.getStartIp(), ipr.getEndIp()));
        }

        SubnetUtils sub = new SubnetUtils(ipr.getStartIp(), ipr.getNetmask());
        SubnetInfo info = sub.getInfo();
        if (!info.isInRange(ipr.getGateway())) {
            throw new ApiMessageInterceptionException(argerr("the gateway[%s] is not in the subnet %s/%s", ipr.getGateway(), ipr.getStartIp(), ipr.getNetmask()));
        }

        if (!NetworkUtils.isIpv4Address(ipr.getStartIp())) {
            throw new ApiMessageInterceptionException(argerr("start ip[%s] is not a IPv4 address", ipr.getStartIp()));
        }

        if (!NetworkUtils.isIpv4Address(ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("end ip[%s] is not a IPv4 address", ipr.getEndIp()));
        }

        if (!NetworkUtils.isIpv4Address(ipr.getGateway())) {
            throw new ApiMessageInterceptionException(argerr("gateway[%s] is not a IPv4 address", ipr.getGateway()));
        }

        if (!NetworkUtils.isNetmaskExcept(ipr.getNetmask(), "0.0.0.0")) {
            throw new ApiMessageInterceptionException(argerr("netmask[%s] is not a netmask, and the IP range netmask cannot be 0.0.0.0", ipr.getNetmask()));
        }

        if (ipr.getStartIp().equals(info.getNetworkAddress()) || ipr.getEndIp().equals(info.getBroadcastAddress())){
            throw new ApiMessageInterceptionException(argerr(
                    "ip allocation can not contain network address or broadcast address")
            );
        }

        long startip = NetworkUtils.ipv4StringToLong(ipr.getStartIp());
        long endip = NetworkUtils.ipv4StringToLong(ipr.getEndIp());
        if (startip > endip) {
            throw new ApiMessageInterceptionException(argerr("start ip[%s] is behind end ip[%s]", ipr.getStartIp(), ipr.getEndIp()));
        }

        long gw = NetworkUtils.ipv4StringToLong(ipr.getGateway());
        if (startip <= gw && gw <= endip) {
            throw new ApiMessageInterceptionException(argerr("gateway[%s] can not be part of range[%s, %s]", ipr.getGateway(), ipr.getStartIp(), ipr.getEndIp()));
        }

        String cidr = ipr.toSubnetUtils().getInfo().getCidrSignature();
        SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
        q.add(IpRangeVO_.l3NetworkUuid, Op.EQ, ipr.getL3NetworkUuid());
        List<IpRangeVO> ranges = q.list();
        for (IpRangeVO r : ranges) {
            if (NetworkUtils.isIpv4RangeOverlap(ipr.getStartIp(), ipr.getEndIp(), r.getStartIp(), r.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("overlap with ip range[uuid:%s, start ip:%s, end ip: %s]", r.getUuid(), r.getStartIp(), r.getEndIp()));
            }

            String rcidr = IpRangeInventory.valueOf(r).toSubnetUtils().getInfo().getCidrSignature();
            if (!cidr.equals(rcidr)) {
                throw new ApiMessageInterceptionException(argerr("multiple CIDR on the same L3 network is not allowed. There has been a IP" +
                                " range[uuid:%s, CIDR:%s], the new IP range[CIDR:%s] is not in the CIDR with the existing one",
                        r.getUuid(), rcidr, cidr));
            }
        }
    }

    private void validate(APIAddIpRangeMsg msg) {
        IpRangeInventory ipr =IpRangeInventory.fromMessage(msg);
        validate(ipr);
    }

    private void validate(APIAddDnsToL3NetworkMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getDns())) {
            throw new ApiMessageInterceptionException(argerr("DNS[%s] is not a IPv4 address", msg.getDns()));
        }

        SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
        q.add(L3NetworkDnsVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(L3NetworkDnsVO_.dns, Op.EQ, msg.getDns());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("there has been a DNS[%s] on L3 network[uuid:%s]", msg.getDns(), msg.getL3NetworkUuid()));
        }
    }
}
