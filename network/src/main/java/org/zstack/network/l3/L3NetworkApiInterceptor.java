package org.zstack.network.l3;


import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.commons.validator.routines.DomainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import java.math.BigInteger;
import java.util.Arrays;
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

    private final static CLogger logger = Utils.getLogger(L3NetworkApiInterceptor.class);

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
        } else if (msg instanceof APIUpdateL3NetworkMsg) {
            validate((APIUpdateL3NetworkMsg) msg);
        } else if (msg instanceof APIAddHostRouteToL3NetworkMsg) {
            validate((APIAddHostRouteToL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveHostRouteFromL3NetworkMsg) {
            validate((APIRemoveHostRouteFromL3NetworkMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeByNetworkCidrMsg) {
            validate((APIAddIpv6RangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeMsg) {
            validate((APIAddIpv6RangeMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIUpdateL3NetworkMsg msg) {
        if (msg.getCategory() == null && msg.getSystem() == null) {
            return;
        }

        Boolean currentSystem = Q.New(L3NetworkVO.class)
                    .select(L3NetworkVO_.system)
                    .eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid())
                    .findValue();
        if (msg.getSystem() != null && msg.getCategory() == null && !msg.getSystem().equals(currentSystem)) {
            throw new ApiMessageInterceptionException(argerr("you must update system and category both"));
        }

        List<L3NetworkCategory> validNetworkCategory = Arrays.asList(L3NetworkCategory.values());
        for (L3NetworkCategory category : validNetworkCategory) {
            if (category.toString().equalsIgnoreCase(msg.getCategory())) {
                msg.setCategory(category.toString());
                break;
            }
        }
        if (msg.getSystem() == null) {
            msg.setSystem(currentSystem);
        }
        if (msg.getCategory() != null) {
            if (L3NetworkCategory.checkSystemAndCategory(msg.getSystem(), L3NetworkCategory.valueOf(msg.getCategory()))) {
                return;
            } else {
                throw new ApiMessageInterceptionException(argerr("not valid combination of system and category," +
                        "only %s are valid", L3NetworkCategory.validCombination));
            }
        }
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
            if (NetworkUtils.isInRange(msg.getRouterInterfaceIp(), ipRangeVO.getStartIp(), ipRangeVO.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("ip[%s] in ip range[uuid:%s, startIp:%s, endIp:%s] which l3 network[%s] attached, this is not allowed",
                        msg.getRouterInterfaceIp(), ipRangeVO.getUuid(), ipRangeVO.getStartIp(), ipRangeVO.getEndIp(), msg.getL3NetworkUuid()));
            }
        }
    }

    private void validate(APICheckIpAvailabilityMsg msg) {
        if (!NetworkUtils.isValidIPAddress(msg.getIp())) {
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

        L3NetworkVO l3Vo = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        if (msg.getStart() == null) {
            if (l3Vo.getIpVersion() == IPv6Constants.IPv6) {
                msg.setStartIp("::");
            } else {
                msg.setStartIp("0.0.0.0");
            }
        }

    }

    private void validate(APIAddIpv6RangeByNetworkCidrMsg msg) {
        if (!IPv6NetworkUtils.isValidUnicastNetworkCidr(msg.getNetworkCidr())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid network cidr", msg.getNetworkCidr()));
        }

        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validateIpv6Range(ipr);
    }

    private void validate(APIAddIpv6RangeMsg msg) {
        if (!IPv6NetworkUtils.isIpv6UnicastAddress(msg.getStartIp())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid ipv6 address", msg.getStartIp()));
        }

        if (!IPv6NetworkUtils.isIpv6UnicastAddress(msg.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid ipv6 address", msg.getEndIp()));
        }

        if (!IPv6NetworkUtils.isIpv6UnicastAddress(msg.getGateway())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid ipv6 address", msg.getGateway()));
        }

        if (!IPv6NetworkUtils.isValidUnicastIpv6Range(msg.getStartIp(), msg.getEndIp(), msg.getGateway(), msg.getPrefixLen())) {
            throw new ApiMessageInterceptionException(argerr("[startIp %s, endIp %s, prefixLen %d, gateway %s] is not a valid ipv6 range",
                    msg.getStartIp(), msg.getEndIp(), msg.getPrefixLen(), msg.getGateway()));
        }

        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validateIpv6Range(ipr);
    }

    private void validateIpv6Range(IpRangeInventory ipr) {
        if (ipr.getPrefixLen() > IPv6Constants.IPV6_PREFIX_LEN_MAX || ipr.getPrefixLen() < IPv6Constants.IPV6_PREFIX_LEN_MIN) {
            throw new ApiMessageInterceptionException(argerr("ip range prefix length is out of range [%d - %d] ",
                    IPv6Constants.IPV6_PREFIX_LEN_MIN, IPv6Constants.IPV6_PREFIX_LEN_MAX));
        }

        L3NetworkVO l3Vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, ipr.getL3NetworkUuid()).find();
        if (l3Vo.getIpVersion() != IPv6Constants.IPv6) {
            throw new ApiMessageInterceptionException(argerr("l3 network [uuid %s: name %s] is not a ipv6 network", l3Vo.getUuid(), l3Vo.getName()));
        }

        List<IpRangeVO> rangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, ipr.getL3NetworkUuid()).list();
        if (rangeVOS != null && !rangeVOS.isEmpty()) {
            if (!rangeVOS.get(0).getAddressMode().equals(ipr.getAddressMode())) {
                throw new ApiMessageInterceptionException(argerr("addressMode[%s] is different from L3Netowork address mode[%s]", ipr.getAddressMode(),
                        rangeVOS.get(0).getAddressMode()));
            }
        }

        if (!ipr.getAddressMode().equals(IPv6Constants.Stateful_DHCP) && ipr.getPrefixLen() != IPv6Constants.IPV6_STATELESS_PREFIX_LEN) {
            throw new ApiMessageInterceptionException(argerr("ipv6 prefix length must be %d for Stateless-DHCP or SLAAC", IPv6Constants.IPV6_STATELESS_PREFIX_LEN));
        }

        List<String> l3Uuids = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, l3Vo.getL2NetworkUuid()).select(L3NetworkVO_.uuid).listValues();
        SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
        q.add(IpRangeVO_.l3NetworkUuid, Op.IN, l3Uuids);
        q.add(IpRangeVO_.ipVersion, Op.EQ, IPv6Constants.IPv6);
        List<IpRangeVO> ranges = q.list();
        for (IpRangeVO r : ranges) {
            if (IPv6NetworkUtils.isIpv6RangeOverlap(ipr.getStartIp(), ipr.getEndIp(), r.getStartIp(), r.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("new ip range [startip :%s, endip :%s] is overlaped with old ip range[startip :%s, endip :%s]",
                        ipr.getStartIp(), ipr.getEndIp(), r.getStartIp(), r.getEndIp()));
            }

            if (!r.getL3NetworkUuid().equals(ipr.getL3NetworkUuid())) {
                continue;
            }

            /* same l3 network can have only 1 cidr */
            if (!r.getNetworkCidr().equals(ipr.getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("new network CIDR [%s] is different from old network cidr [%s]",
                        r.getNetworkCidr(), ipr.getNetworkCidr()));
            }
        }

        /* ipranges of same l3 network must have same gateway */
        List<IpRangeVO> l3IpRanges = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, ipr.getL3NetworkUuid()).list();
        for (IpRangeVO r : l3IpRanges) {
            if (!r.getGateway().equals(ipr.getGateway())) {
                throw new ApiMessageInterceptionException(argerr("new add ip range gateway %s is different from old gateway %s", ipr.getGateway(), r.getGateway()));
            }
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

            if (msg.getGateway() != null && !(msg.getGateway().equals(subnet.getLowAddress()) || msg.getGateway().equals(subnet.getHighAddress()))) {
                throw new ApiMessageInterceptionException(argerr("%s is not the first or last address of the cidr %s", msg.getGateway(), msg.getNetworkCidr()));
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

        List<L3NetworkCategory> validNetworkCategory = Arrays.asList(L3NetworkCategory.values());
        for (L3NetworkCategory category : validNetworkCategory) {
            if (category.toString().equalsIgnoreCase(msg.getCategory())) {
                msg.setCategory(category.toString());
                break;
            }
        }

        if (L3NetworkCategory.checkSystemAndCategory(msg.isSystem(), L3NetworkCategory.valueOf(msg.getCategory()))) {
            return;
        } else {
            throw new ApiMessageInterceptionException(argerr("not valid combination of system and category," +
                    "only %s are valid", L3NetworkCategory.validCombination));
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
        L3NetworkVO l3Vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, ipr.getL3NetworkUuid()).find();
        if (l3Vo.getIpVersion() != IPv6Constants.IPv4) {
            throw new ApiMessageInterceptionException(argerr("l3 network [uuid %s: name %s] is not a ipv4 network", l3Vo.getUuid(), l3Vo.getName()));
        }

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

        if (!info.isInRange(ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("the endip[%s] is not in the subnet %s/%s", ipr.getEndIp(), ipr.getStartIp(), ipr.getNetmask()));
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
        List<String> l3Uuids = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, l3Vo.getL2NetworkUuid()).select(L3NetworkVO_.uuid).listValues();
        SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
        q.add(IpRangeVO_.l3NetworkUuid, Op.IN, l3Uuids);
        q.add(IpRangeVO_.ipVersion, Op.EQ, IPv6Constants.IPv4);
        List<IpRangeVO> ranges = q.list();
        for (IpRangeVO r : ranges) {
            if (NetworkUtils.isIpv4RangeOverlap(ipr.getStartIp(), ipr.getEndIp(), r.getStartIp(), r.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("overlap with ip range[uuid:%s, start ip:%s, end ip: %s]", r.getUuid(), r.getStartIp(), r.getEndIp()));
            }

            if (!r.getL3NetworkUuid().equals(ipr.getL3NetworkUuid())) {
                continue;
            }

            /* same l3 network can have only 1 cidr */
            String rcidr = IpRangeInventory.valueOf(r).toSubnetUtils().getInfo().getCidrSignature();
            if (!cidr.equals(rcidr)) {
                throw new ApiMessageInterceptionException(argerr("multiple CIDR on the same L3 network is not allowed. There has been a IP" +
                                " range[uuid:%s, CIDR:%s], the new IP range[CIDR:%s] is not in the CIDR with the existing one",
                        r.getUuid(), rcidr, cidr));
            }
        }

        /* ipranges of same l3 network must have same gateway */
        List<IpRangeVO> l3IpRanges = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, ipr.getL3NetworkUuid()).list();
        for (IpRangeVO r : l3IpRanges) {
            if (!r.getGateway().equals(ipr.getGateway())) {
                throw new ApiMessageInterceptionException(argerr("new add ip range gateway %s is different from old gateway %s", ipr.getGateway(), r.getGateway()));
            }
        }
    }

    private void validate(APIAddIpRangeMsg msg) {
        IpRangeInventory ipr =IpRangeInventory.fromMessage(msg);
        validate(ipr);
    }

    private void validateIpAddress(String l3NetworkUuid, String ip, String manner) {
        L3NetworkVO l3VO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3NetworkUuid).find();
        if (l3VO == null) {
            return;
        }

        if (l3VO.getIpVersion() == IPv6Constants.IPv4) {
            if (!NetworkUtils.isIpv4Address(ip)) {
                throw new ApiMessageInterceptionException(argerr("%s[%s] is not a IPv4 address", manner, ip));
            }
        } else {
            if (!IPv6NetworkUtils.isIpv6Address(ip)) {
                throw new ApiMessageInterceptionException(argerr("%s[%s] is not a IPv6 address", manner, ip));
            }
        }
    }

    private void validate(APIAddDnsToL3NetworkMsg msg) {
        validateIpAddress(msg.getL3NetworkUuid(), msg.getDns(), "DNS");

        SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
        q.add(L3NetworkDnsVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(L3NetworkDnsVO_.dns, Op.EQ, msg.getDns());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("there has been a DNS[%s] on L3 network[uuid:%s]", msg.getDns(), msg.getL3NetworkUuid()));
        }
    }

    private void validate(APIAddHostRouteToL3NetworkMsg msg) {
        if (!NetworkUtils.isCidr(msg.getPrefix())) {
            throw new ApiMessageInterceptionException(argerr("prefix [%s] is not a IPv4 network cidr", msg.getL3NetworkUuid()));
        }

        if (!NetworkUtils.isIpv4Address(msg.getNexthop())) {
            throw new ApiMessageInterceptionException(argerr("nexthop[%s] is not a IPv4 address", msg.getNexthop()));
        }

        SimpleQuery<L3NetworkHostRouteVO> q = dbf.createQuery(L3NetworkHostRouteVO.class);
        q.add(L3NetworkHostRouteVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(L3NetworkHostRouteVO_.prefix, Op.EQ, msg.getPrefix());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("there has been a hostroute for prefix[%s] on L3 network[uuid:%s]", msg.getPrefix(), msg.getL3NetworkUuid()));
        }
    }

    private void validate(APIRemoveHostRouteFromL3NetworkMsg msg) {
        if (!NetworkUtils.isCidr(msg.getPrefix())) {
            throw new ApiMessageInterceptionException(argerr("prefix [%s] is not a IPv4 network cidr", msg.getL3NetworkUuid()));
        }

        SimpleQuery<L3NetworkHostRouteVO> q = dbf.createQuery(L3NetworkHostRouteVO.class);
        q.add(L3NetworkHostRouteVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(L3NetworkHostRouteVO_.prefix, Op.EQ, msg.getPrefix());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(operr("there is no hostroute for prefix[%s] on L3 network[uuid:%s]", msg.getPrefix(), msg.getL3NetworkUuid()));
        }
    }
}
