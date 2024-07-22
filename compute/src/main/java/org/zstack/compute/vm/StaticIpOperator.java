package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.TagUtils;
import org.zstack.utils.network.NicIpAddressInfo;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/5/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StaticIpOperator implements SystemTagCreateMessageValidator, SystemTagValidator {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private TagManager tagMgr;

    public Map<String, List<String>> getStaticIpbyVmUuid(String vmUuid) {
        Map<String, List<String>> ret = new HashMap<String, List<String>>();

        List<Map<String, String>> tokenList = VmSystemTags.STATIC_IP.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String l3Uuid = tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            String ip = tokens.get(VmSystemTags.STATIC_IP_TOKEN);
            ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
            ret.computeIfAbsent(l3Uuid, k -> new ArrayList<>()).add(ip);
        }

        return ret;
    }

    public Map<String, NicIpAddressInfo> getNicNetworkInfoByVmUuid(String vmUuid) {
        return getNicNetworkInfoBySystemTag(Q.New(SystemTagVO.class).select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceUuid, vmUuid).listValues());
    }

    public Map<String, NicIpAddressInfo> getNicNetworkInfoBySystemTag(List<String> systemTags) {
        Map<String, NicIpAddressInfo> ret = new HashMap<>();
        if (systemTags == null || systemTags.isEmpty()) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(VmSystemTags.STATIC_IP.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
                NicIpAddressInfo nicIpAddressInfo = ret.get(l3Uuid);
                if (nicIpAddressInfo == null) {
                    ret.put(l3Uuid, new NicIpAddressInfo("", "", "",
                            "", "", ""));
                }
                String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
                ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
                if (NetworkUtils.isIpv4Address(ip)) {
                    ret.get(l3Uuid).ipv4Address = ip;
                } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
                    ret.get(l3Uuid).ipv6Address = ip;
                } else {
                    throw new ApiMessageInterceptionException(argerr("the static IP[%s] format error", ip));
                }
            }
        }

        if (ret.isEmpty()) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(VmSystemTags.IPV4_GATEWAY.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv4Gateway = token.get(VmSystemTags.IPV4_GATEWAY_TOKEN);
            }
            if(VmSystemTags.IPV4_NETMASK.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_NETMASK.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv4Netmask = token.get(VmSystemTags.IPV4_NETMASK_TOKEN);
            }
            if(VmSystemTags.IPV6_GATEWAY.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv6Gateway = IPv6NetworkUtils.ipv6TagValueToAddress(token.get(VmSystemTags.IPV6_GATEWAY_TOKEN));
            }
            if(VmSystemTags.IPV6_PREFIX.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_PREFIX.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv6Prefix = token.get(VmSystemTags.IPV6_PREFIX_TOKEN);
            }
        }

        return ret;
    }

    public Map<String, List<String>> getStaticIpbySystemTag(List<String> systemTags) {
        Map<String, List<String>> ret = new HashMap<>();

        if (systemTags == null) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(!VmSystemTags.STATIC_IP.isMatch(sysTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), sysTag);
            String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
            ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
            ret.computeIfAbsent(l3Uuid, k -> new ArrayList<>()).add(ip);
        }

        return ret;
    }

    public void setStaticIp(String vmUuid, String l3Uuid, String ip) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.uuid, SystemTagVO_.tag);
        q.add(SystemTagVO_.resourceType, Op.EQ, VmInstanceVO.class.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, vmUuid);
        q.add(SystemTagVO_.tag, Op.LIKE, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
        final List<Tuple> tags = q.listTuple();

        String tagUuid = null;
        boolean isIpv4 = NetworkUtils.isIpv4Address(ip);
        if (tags != null && !tags.isEmpty()) {
            for (Tuple tag : tags) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), (String)tag.get(1));
                String oldIp = token.get(VmSystemTags.STATIC_IP_TOKEN);
                oldIp = IPv6NetworkUtils.ipv6TagValueToAddress(oldIp);
                boolean isIpv4Tag = NetworkUtils.isIpv4Address(oldIp);
                if (isIpv4 == isIpv4Tag) { /* compare ip version */
                    tagUuid = (String) tag.get(0);
                    break;
                }
            }
        }

        /* '::' is token used by systemtag, replace with "--" */
        ip = IPv6NetworkUtils.ipv6AddessToTagValue(ip);
        if (tagUuid == null) {
            SystemTagCreator creator = VmSystemTags.STATIC_IP.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(
                    e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                    e(VmSystemTags.STATIC_IP_TOKEN, ip)
            ));
            creator.create();
        } else {
            VmSystemTags.STATIC_IP.updateByTagUuid(tagUuid, VmSystemTags.STATIC_IP.instantiateTag(map(
                    e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                    e(VmSystemTags.STATIC_IP_TOKEN, ip)
            )));
        }
    }

    public void deleteStaticIpByVmUuidAndL3Uuid(String vmUuid, String l3Uuid) {
        VmSystemTags.STATIC_IP.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
    }

    public void deleteStaticIpByVmUuidAndL3Uuid(String vmUuid, String l3Uuid, String ip) {
        VmSystemTags.STATIC_IP.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid), e(VmSystemTags.STATIC_IP_TOKEN, ip))
        )));
    }

    public void deleteStaticIpByL3NetworkUuid(String l3Uuid) {
        VmSystemTags.STATIC_IP.delete(null, VmSystemTags.STATIC_IP.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                e(VmSystemTags.STATIC_IP_TOKEN, "%")
        )));
    }

    public Map<Integer, String> getNicStaticIpMap(List<String> nicStaticIpList) {
        Map<Integer, String> nicStaticIpMap = new HashMap<>();
        if (nicStaticIpList != null) {
            for (String ip : nicStaticIpList) {
                if (NetworkUtils.isIpv4Address(ip)) {
                    nicStaticIpMap.put(IPv6Constants.IPv4, ip);
                } else {
                    nicStaticIpMap.put(IPv6Constants.IPv6, ip);
                }
            }
        }

        return nicStaticIpMap;
    }

    public void setIpChange(String vmUuid, String l3Uuid) {
        SystemTagCreator creator = VmSystemTags.VM_IP_CHANGED.newSystemTagCreator(vmUuid);
        creator.recreate = true;
        creator.inherent = false;
        creator.setTagByTokens(map(
                e(VmSystemTags.VM_IP_CHANGED_TOKEN, l3Uuid)
        ));
        creator.create();
    }

    public void deleteIpChange(String vmUuid) {
        VmSystemTags.VM_IP_CHANGED.delete(vmUuid, VmInstanceVO.class);
    }

    public boolean isIpChange(String vmUuid, String l3Uuid) {
        List<Map<String, String>> tokenList = VmSystemTags.VM_IP_CHANGED.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String uuid = tokens.get(VmSystemTags.VM_IP_CHANGED_TOKEN);
            if (uuid.equals(l3Uuid)) {
                return true;
            }
        }

        return false;
    }

    public Boolean checkIpRangeConflict(VmNicVO nicVO){
        if (Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, nicVO.getL3NetworkUuid()).list().isEmpty()) {
            return Boolean.FALSE;
        }
        if (getIpRangeUuid(nicVO.getL3NetworkUuid(), nicVO.getIp()) == null) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public String getIpRangeUuid(String l3Uuid, String ip) {
        if (IPv6NetworkUtils.isIpv6Address(ip)) {
            List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class)
                    .eq(IpRangeVO_.l3NetworkUuid, l3Uuid)
                    .eq(IpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
            for (IpRangeVO ipr : ipRangeVOS) {
                if (IPv6NetworkUtils.isIpv6InRange(ip, ipr.getStartIp(), ipr.getEndIp())) {
                    return ipr.getUuid();
                }
            }
        } else if (NetworkUtils.isIpv4Address(ip)) {
            List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class)
                    .eq(IpRangeVO_.l3NetworkUuid, l3Uuid)
                    .eq(IpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
            for (IpRangeVO ipr : ipRangeVOS) {
                if (NetworkUtils.isInRange(ip, ipr.getStartIp(), ipr.getEndIp())) {
                    return ipr.getUuid();
                }
            }
        }

        return null;
    }

    public void checkIpAvailability(String l3Uuid, String ip) {
        CheckIpAvailabilityMsg cmsg = new CheckIpAvailabilityMsg();
        cmsg.setIp(ip);
        cmsg.setL3NetworkUuid(l3Uuid);
        bus.makeLocalServiceId(cmsg, L3NetworkConstant.SERVICE_ID);
        MessageReply r = bus.call(cmsg);
        if (!r.isSuccess()) {
            throw new ApiMessageInterceptionException(argerr(r.getError().getDetails()));
        }

        CheckIpAvailabilityReply cr = r.castReply();
        if (!cr.isAvailable()) {
            throw new ApiMessageInterceptionException(argerr("IP[%s] is not available on the L3 network[uuid:%s] because: %s", ip, l3Uuid, cr.getReason()));
        }
    }

    @Override
    public void validateSystemTagInCreateMessage(APICreateMessage msg) {
        Map<String, NicIpAddressInfo> staticIps = getNicNetworkInfoBySystemTag(msg.getSystemTags());
        List<String> newSystags = new ArrayList<>();
        for (Map.Entry<String, NicIpAddressInfo> e : staticIps.entrySet()) {
            String l3Uuid = e.getKey();
            NicIpAddressInfo nicIp = e.getValue();

            if (!StringUtils.isEmpty(nicIp.ipv4Address)) {
                checkIpAvailability(l3Uuid, nicIp.ipv4Address);
            }

            if (!StringUtils.isEmpty(nicIp.ipv6Address)) {
                checkIpAvailability(l3Uuid, nicIp.ipv6Address);
            }

            L3NetworkVO l3NetworkVO = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
            if (l3NetworkVO.enableIpAddressAllocation()) {
                continue;
            }

            if (!StringUtils.isEmpty(nicIp.ipv4Address)) {
                NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                        .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4)
                        .limit(1).find();
                if (ipRangeVO == null) {
                    if (StringUtils.isEmpty(nicIp.ipv4Netmask)) {
                        throw new ApiMessageInterceptionException(operr("netmask must be set"));
                    }
                } else {
                    if (StringUtils.isEmpty(nicIp.ipv4Netmask)) {
                        newSystags.add(VmSystemTags.IPV4_NETMASK.instantiateTag(
                                map(e(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN, l3Uuid),
                                        e(VmSystemTags.IPV4_NETMASK_TOKEN, ipRangeVO.getNetmask()))
                        ));
                    } else if (!nicIp.ipv4Netmask.equals(ipRangeVO.getNetmask())) {
                        throw new ApiMessageInterceptionException(operr("netmask error, expect: %s, got: %s",
                                    ipRangeVO.getNetmask(), nicIp.ipv4Netmask));
                    }

                    if (StringUtils.isEmpty(nicIp.ipv4Gateway)) {
                        newSystags.add(VmSystemTags.IPV4_GATEWAY.instantiateTag(
                                map(e(VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN, l3Uuid),
                                        e(VmSystemTags.IPV4_GATEWAY_TOKEN, ipRangeVO.getGateway()))
                        ));
                    } else if (!nicIp.ipv4Gateway.equals(ipRangeVO.getGateway())) {
                        throw new ApiMessageInterceptionException(operr("gateway error, expect: %s, got: %s",
                                ipRangeVO.getGateway(), nicIp.ipv4Gateway));
                    }
                }
            }

            if (!StringUtils.isEmpty(nicIp.ipv6Address)) {
                NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                        .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6)
                        .limit(1).find();
                if (ipRangeVO == null) {
                    if (StringUtils.isEmpty(nicIp.ipv6Prefix)) {
                        throw new ApiMessageInterceptionException(operr("ipv6 prefix length must be set"));
                    }
                } else {
                    if (StringUtils.isEmpty(nicIp.ipv6Prefix)) {
                        newSystags.add(VmSystemTags.IPV6_PREFIX.instantiateTag(
                                map(e(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN, l3Uuid),
                                        e(VmSystemTags.IPV6_PREFIX_TOKEN, ipRangeVO.getPrefixLen()))
                        ));
                    } else if (!nicIp.ipv6Prefix.equals(ipRangeVO.getPrefixLen().toString())) {
                        throw new ApiMessageInterceptionException(operr("ipv6 prefix length error, expect: %s, got: %s",
                                ipRangeVO.getPrefixLen(), nicIp.ipv6Prefix));
                    }

                    if (StringUtils.isEmpty(nicIp.ipv6Gateway)) {
                        newSystags.add(VmSystemTags.IPV6_GATEWAY.instantiateTag(
                                map(e(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN, l3Uuid),
                                        e(VmSystemTags.IPV6_GATEWAY_TOKEN,
                                                IPv6NetworkUtils.ipv6AddressToTagValue(ipRangeVO.getGateway())))
                        ));
                    } else if (!nicIp.ipv6Gateway.equals(ipRangeVO.getGateway())) {
                        throw new ApiMessageInterceptionException(operr("gateway error, expect: %s, got: %s",
                                ipRangeVO.getGateway(), nicIp.ipv6Gateway));
                    }
                }
            }

            if (!newSystags.isEmpty()) {
                msg.getSystemTags().addAll(newSystags);
            }
        }
    }

    @Override
    public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
        if (VmSystemTags.STATIC_IP.isMatch(systemTag)) {
            Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), systemTag);
            String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
            checkIpAvailability(l3Uuid, IPv6NetworkUtils.ipv6TagValueToAddress(ip));
        }
    }

    public void installStaticIpValidator() {
        StaticIpOperator staticIpValidator = new StaticIpOperator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), staticIpValidator);
        //VmSystemTags.STATIC_IP.installValidator(staticIpValidator);
    }
}
