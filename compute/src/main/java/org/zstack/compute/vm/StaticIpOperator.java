package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
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
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.l3.IpRangeHelper;
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
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

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
                ret.computeIfAbsent(l3Uuid, k -> new NicIpAddressInfo("", "", "",
                        "", "", ""));
                String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
                ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
                if (NetworkUtils.isIpv4Address(ip)) {
                    ret.get(l3Uuid).ipv4Address = ip;
                } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
                    ret.get(l3Uuid).ipv6Address = ip;
                } else {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10307, "the static IP[%s] format error", ip));
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
            } else if(VmSystemTags.IPV4_NETMASK.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_NETMASK.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv4Netmask = token.get(VmSystemTags.IPV4_NETMASK_TOKEN);
            } else if(VmSystemTags.IPV6_GATEWAY.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv6Gateway = IPv6NetworkUtils.ipv6TagValueToAddress(token.get(VmSystemTags.IPV6_GATEWAY_TOKEN));
            } else if(VmSystemTags.IPV6_PREFIX.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_PREFIX.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv6Prefix = token.get(VmSystemTags.IPV6_PREFIX_TOKEN);
            } else if(VmSystemTags.STATIC_DNS.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_DNS.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.STATIC_DNS_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                String dnsStr = token.get(VmSystemTags.STATIC_DNS_TOKEN);
                if (dnsStr != null && !dnsStr.isEmpty()) {
                    // Convert back from tag value: replace '--' with '::' for IPv6 addresses
                    List<String> dnsList = new ArrayList<>();
                    for (String dns : dnsStr.split(",")) {
                        dnsList.add(IPv6NetworkUtils.ipv6TagValueToAddress(dns));
                    }
                    ret.get(l3Uuid).dnsAddresses = dnsList;
                }
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

    public void setStaticDns(String vmUuid, String l3Uuid, List<String> dnsAddresses) {
        if (dnsAddresses == null || dnsAddresses.isEmpty()) {
            deleteStaticDnsByVmUuidAndL3Uuid(vmUuid, l3Uuid);
            return;
        }

        // Convert IPv6 addresses: replace '::' with '--' to avoid conflict with system tag delimiter
        List<String> tagSafeDns = new ArrayList<>();
        for (String dns : dnsAddresses) {
            tagSafeDns.add(IPv6NetworkUtils.ipv6AddessToTagValue(dns));
        }
        String dnsStr = String.join(",", tagSafeDns);

        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.uuid);
        q.add(SystemTagVO_.resourceType, Op.EQ, VmInstanceVO.class.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, vmUuid);
        q.add(SystemTagVO_.tag, Op.LIKE, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_DNS.instantiateTag(
                map(e(VmSystemTags.STATIC_DNS_L3_UUID_TOKEN, l3Uuid))
        )));
        String tagUuid = q.findValue();

        if (tagUuid == null) {
            SystemTagCreator creator = VmSystemTags.STATIC_DNS.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(
                    e(VmSystemTags.STATIC_DNS_L3_UUID_TOKEN, l3Uuid),
                    e(VmSystemTags.STATIC_DNS_TOKEN, dnsStr)
            ));
            creator.create();
        } else {
            VmSystemTags.STATIC_DNS.updateByTagUuid(tagUuid, VmSystemTags.STATIC_DNS.instantiateTag(map(
                    e(VmSystemTags.STATIC_DNS_L3_UUID_TOKEN, l3Uuid),
                    e(VmSystemTags.STATIC_DNS_TOKEN, dnsStr)
            )));
        }
    }

    public void deleteStaticDnsByVmUuidAndL3Uuid(String vmUuid, String l3Uuid) {
        VmSystemTags.STATIC_DNS.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_DNS.instantiateTag(
                map(e(VmSystemTags.STATIC_DNS_L3_UUID_TOKEN, l3Uuid))
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

    public Boolean isNicIpInL3IpRanges(VmNicVO nicVO){
        if (Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, nicVO.getL3NetworkUuid()).list().isEmpty()) {
            return Boolean.TRUE;
        }
        if (getIpRangeUuid(nicVO.getL3NetworkUuid(), nicVO.getIp()) == null) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
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

    public NormalIpRangeVO findMatchedNormalIpRange(String l3Uuid, String ip) {
        String rangeUuid = getIpRangeUuid(l3Uuid, ip);
        if (rangeUuid == null) {
            return null;
        }
        return dbf.findByUuid(rangeUuid, NormalIpRangeVO.class);
    }

    public void checkIpAvailability(String l3Uuid, String ip) {
        CheckIpAvailabilityMsg cmsg = new CheckIpAvailabilityMsg();
        cmsg.setIp(ip);
        cmsg.setL3NetworkUuid(l3Uuid);
        cmsg.setIpRangeCheck(false);
        bus.makeLocalServiceId(cmsg, L3NetworkConstant.SERVICE_ID);
        MessageReply r = bus.call(cmsg);
        if (!r.isSuccess()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10308, r.getError().getDetails()));
        }

        CheckIpAvailabilityReply cr = r.castReply();
        if (!cr.isAvailable()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10309, "IP[%s] is not available on the L3 network[uuid:%s] because: %s", ip, l3Uuid, cr.getReason()));
        }
    }


    @Override
    public void validateSystemTagInCreateMessage(APICreateMessage msg) {
        validateSystemTagInApiMessage(msg);
    }

    // ================================================================
    //  Context classes for unified resolve logic
    // ================================================================

    /**
     * Describes the role of the NIC being resolved, used by resolve methods
     * to decide whether gateway is mandatory.
     */
    public static class NicRoleContext {
        public final boolean isDefaultNic;
        public final boolean isOnlyNic;

        public NicRoleContext(boolean isDefaultNic, boolean isOnlyNic) {
            this.isDefaultNic = isDefaultNic;
            this.isOnlyNic = isOnlyNic;
        }
    }

    /**
     * Holds existing UsedIpVO per (l3Uuid, ipVersion) for case(d) reuse logic.
     * Only APISetVmStaticIpMsg populates this; APIChangeVmNicNetworkMsg passes empty.
     */
    public static class ExistingIpContext {
        private final Map<String, UsedIpVO> ipv4Map = new HashMap<>();
        private final Map<String, UsedIpVO> ipv6Map = new HashMap<>();

        public void putIpv4(String l3Uuid, UsedIpVO vo) {
            if (vo != null) {
                ipv4Map.put(l3Uuid, vo);
            }
        }

        public void putIpv6(String l3Uuid, UsedIpVO vo) {
            if (vo != null) {
                ipv6Map.put(l3Uuid, vo);
            }
        }

        public UsedIpVO getIpv4(String l3Uuid) {
            return ipv4Map.get(l3Uuid);
        }

        public UsedIpVO getIpv6(String l3Uuid) {
            return ipv6Map.get(l3Uuid);
        }
    }

    // ================================================================
    //  Unified resolve methods (migrated from VmInstanceApiInterceptor)
    // ================================================================

    /**
     * Determine whether to use the NIC's existing IPv4 parameters (netmask/gateway).
     * Condition: existingIp is non-null with non-empty netmask and non-empty gateway,
     * and the IP falls within the CIDR formed by existingIp's gateway + netmask.
     */
    public boolean shouldUseExistingIpv4(String ip, UsedIpVO existingIp) {
        if (existingIp == null || StringUtils.isEmpty(existingIp.getNetmask())) {
            return false;
        }
        if (StringUtils.isEmpty(existingIp.getGateway())) {
            return false;
        }
        try {
            SubnetUtils.SubnetInfo info = NetworkUtils.getSubnetInfo(
                    new SubnetUtils(existingIp.getGateway(), existingIp.getNetmask()));
            return NetworkUtils.isIpv4InRange(ip, info.getLowAddress(), info.getHighAddress());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determine whether to use the NIC's existing IPv6 parameters (prefix/gateway).
     * Condition: existingIp is non-null with non-null prefixLen and non-empty gateway,
     * and the IP falls within the CIDR formed by existingIp's gateway + prefixLen.
     */
    public boolean shouldUseExistingIpv6(String ip6, UsedIpVO existingIp) {
        if (existingIp == null || existingIp.getPrefixLen() == null) {
            return false;
        }
        if (StringUtils.isEmpty(existingIp.getGateway())) {
            return false;
        }
        try {
            return IPv6NetworkUtils.isIpv6InCidrRange(ip6,
                    existingIp.getGateway() + "/" + existingIp.getPrefixLen());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolve IPv4 netmask and gateway based on 4 cases:
     * (a) Both netmask+gateway provided: validate gateway is in CIDR(ip/netmask), then use user input
     * (b) Gateway provided, no netmask: if ip and gateway both in L3 CIDR, use CIDR netmask; else error
     * (c) Netmask provided, no gateway: if netmask == CIDR netmask, use CIDR gateway; else gateway=""
     * (d) Neither provided: if existingIp usable, use it; else if in L3 CIDR, use CIDR; else error
     */
    public String[] resolveIpv4NetmaskAndGateway(String ip, String userNetmask, String userGateway,
            List<NormalIpRangeVO> ipv4Ranges, NicRoleContext nicRole, UsedIpVO existingIp) {
        boolean hasNetmask = StringUtils.isNotEmpty(userNetmask);
        boolean hasGateway = StringUtils.isNotEmpty(userGateway);

        // case (a): both provided — validate gateway is in the CIDR formed by ip/netmask
        if (hasNetmask && hasGateway) {
            String cidr = NetworkUtils.getCidrFromIpMask(ip, userNetmask);
            if (!NetworkUtils.isIpv4InCidr(userGateway, cidr)) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10329,
                        "gateway[%s] is not in the CIDR[%s] formed by IP[%s] and netmask[%s]",
                        userGateway, cidr, ip, userNetmask));
            }
            return new String[]{userNetmask, userGateway};
        }

        NormalIpRangeVO matchedRange = IpRangeHelper.findIpRangeByCidr(ip, ipv4Ranges);

        // case (b): gateway provided, no netmask
        if (hasGateway) {
            if (matchedRange != null && matchedRange.getNetworkCidr() != null
                    && NetworkUtils.isIpv4InCidr(userGateway, matchedRange.getNetworkCidr())) {
                return new String[]{matchedRange.getNetmask(), userGateway};
            }
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10323,
                    "gateway[%s] is provided but IP[%s] and gateway are not both in L3 network CIDR, netmask must be specified",
                    userGateway, ip));
        }

        // case (c): netmask provided, no gateway
        if (hasNetmask) {
            if (matchedRange != null && userNetmask.equals(matchedRange.getNetmask())) {
                return new String[]{matchedRange.getNetmask(), matchedRange.getGateway()};
            }
            return new String[]{userNetmask, ""};
        }

        // case (d): neither provided
        if (existingIp != null && shouldUseExistingIpv4(ip, existingIp)) {
            return new String[]{existingIp.getNetmask(), existingIp.getGateway()};
        }
        if (matchedRange != null) {
            return new String[]{matchedRange.getNetmask(), matchedRange.getGateway()};
        }
        throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10325,
                "IP[%s] is outside all L3 network CIDRs and no existing IP parameters available, netmask and gateway must be specified",
                ip));
    }

    /**
     * Resolve IPv6 prefix and gateway based on 4 cases (mirrors IPv4 logic):
     * (a) Both prefix+gateway provided: validate gateway is in CIDR(ip6/prefix), then use user input
     * (b) Gateway provided, no prefix: if ip and gateway both in L3 CIDR, use CIDR prefix; else error
     * (c) Prefix provided, no gateway: if prefix == CIDR prefix, use CIDR gateway; else if default/sole NIC, error; else gateway=""
     * (d) Neither provided: if existingIp usable, use it; else if in L3 CIDR, use CIDR; else error
     */
    public String[] resolveIpv6PrefixAndGateway(String ip6, String userPrefix, String userGateway,
            List<NormalIpRangeVO> ipv6Ranges, NicRoleContext nicRole, UsedIpVO existingIp) {
        boolean hasPrefix = StringUtils.isNotEmpty(userPrefix);
        boolean hasGateway = StringUtils.isNotEmpty(userGateway);

        // case (a): both provided — validate gateway is in the CIDR formed by ip6/prefix
        if (hasPrefix && hasGateway) {
            String cidr = ip6 + "/" + userPrefix;
            if (!IPv6NetworkUtils.isIpv6InCidrRange(userGateway, cidr)) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10330,
                        "gateway[%s] is not in the CIDR[%s] formed by IPv6[%s] and prefix[%s]",
                        userGateway, cidr, ip6, userPrefix));
            }
            return new String[]{userPrefix, userGateway};
        }

        NormalIpRangeVO matchedRange = IpRangeHelper.findIpRangeByCidr(ip6, ipv6Ranges);

        // case (b): gateway provided, no prefix
        if (hasGateway) {
            if (matchedRange != null && matchedRange.getNetworkCidr() != null
                    && IPv6NetworkUtils.isIpv6InCidrRange(userGateway, matchedRange.getNetworkCidr())) {
                return new String[]{matchedRange.getPrefixLen().toString(), userGateway};
            }
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10326,
                    "gateway[%s] is provided but IPv6[%s] and gateway are not both in L3 network CIDR, prefix must be specified",
                    userGateway, ip6));
        }

        // case (c): prefix provided, no gateway
        if (hasPrefix) {
            if (matchedRange != null && userPrefix.equals(matchedRange.getPrefixLen().toString())) {
                return new String[]{matchedRange.getPrefixLen().toString(), matchedRange.getGateway()};
            }
            if (nicRole.isDefaultNic || nicRole.isOnlyNic) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10327,
                        "prefix[%s] does not match L3 CIDR prefix and the NIC is the default or sole network, gateway must be specified",
                        userPrefix));
            }
            return new String[]{userPrefix, ""};
        }

        // case (d): neither provided
        if (existingIp != null && shouldUseExistingIpv6(ip6, existingIp)) {
            return new String[]{existingIp.getPrefixLen().toString(), existingIp.getGateway()};
        }
        if (matchedRange != null) {
            return new String[]{matchedRange.getPrefixLen().toString(), matchedRange.getGateway()};
        }
        throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10328,
                "IPv6[%s] is outside all L3 network CIDRs and no existing IP parameters available, prefix and gateway must be specified",
                ip6));
    }

    // ================================================================
    //  IP availability validation (extracted from old fillUpStaticIpInfoToVmNics)
    // ================================================================

    /**
     * Validate that all static IPs are available on their respective L3 networks.
     */
    public void validateIpAvailability(Map<String, NicIpAddressInfo> staticIps) {
        for (Map.Entry<String, NicIpAddressInfo> e : staticIps.entrySet()) {
            String l3Uuid = e.getKey();
            NicIpAddressInfo nicIp = e.getValue();

            if (!StringUtils.isEmpty(nicIp.ipv4Address)) {
                checkIpAvailability(l3Uuid, nicIp.ipv4Address);
            }

            if (!StringUtils.isEmpty(nicIp.ipv6Address)) {
                checkIpAvailability(l3Uuid, nicIp.ipv6Address);
            }
        }
    }

    // ================================================================
    //  fillUpStaticIpInfoToVmNics — orchestration layer
    // ================================================================

    /**
     * New signature: resolves netmask/gateway (or prefix/gateway) for each static IP entry,
     * using the unified resolve methods with NicRoleContext and ExistingIpContext.
     * Returns system tags to be added to the message.
     */
    public List<String> fillUpStaticIpInfoToVmNics(Map<String, NicIpAddressInfo> staticIps,
            NicRoleContext nicRole, ExistingIpContext existingIpCtx) {
        List<String> newSystags = new ArrayList<>();
        for (Map.Entry<String, NicIpAddressInfo> entry : staticIps.entrySet()) {
            String l3Uuid = entry.getKey();
            NicIpAddressInfo nicIp = entry.getValue();

            // Resolve IPv4 netmask/gateway
            if (!StringUtils.isEmpty(nicIp.ipv4Address)) {
                List<NormalIpRangeVO> ipv4Ranges = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                        .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
                UsedIpVO existingIpv4 = existingIpCtx != null ? existingIpCtx.getIpv4(l3Uuid) : null;

                String[] ipv4Result = resolveIpv4NetmaskAndGateway(nicIp.ipv4Address,
                        nicIp.ipv4Netmask, nicIp.ipv4Gateway, ipv4Ranges, nicRole, existingIpv4);

                newSystags.add(VmSystemTags.IPV4_NETMASK.instantiateTag(
                        map(e(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN, l3Uuid),
                                e(VmSystemTags.IPV4_NETMASK_TOKEN, ipv4Result[0]))));
                if (!StringUtils.isEmpty(ipv4Result[1])) {
                    newSystags.add(VmSystemTags.IPV4_GATEWAY.instantiateTag(
                            map(e(VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN, l3Uuid),
                                    e(VmSystemTags.IPV4_GATEWAY_TOKEN, ipv4Result[1]))));
                }
            }

            // Resolve IPv6 prefix/gateway
            if (!StringUtils.isEmpty(nicIp.ipv6Address)) {
                List<NormalIpRangeVO> ipv6Ranges = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                        .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
                UsedIpVO existingIpv6 = existingIpCtx != null ? existingIpCtx.getIpv6(l3Uuid) : null;

                String[] ipv6Result = resolveIpv6PrefixAndGateway(nicIp.ipv6Address,
                        nicIp.ipv6Prefix, nicIp.ipv6Gateway, ipv6Ranges, nicRole, existingIpv6);

                newSystags.add(VmSystemTags.IPV6_PREFIX.instantiateTag(
                        map(e(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN, l3Uuid),
                                e(VmSystemTags.IPV6_PREFIX_TOKEN, ipv6Result[0]))));
                if (!StringUtils.isEmpty(ipv6Result[1])) {
                    newSystags.add(VmSystemTags.IPV6_GATEWAY.instantiateTag(
                            map(e(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN, l3Uuid),
                                    e(VmSystemTags.IPV6_GATEWAY_TOKEN,
                                            IPv6NetworkUtils.ipv6AddressToTagValue(ipv6Result[1])))));
                }
            }
        }

        return newSystags;
    }

    /**
     * Legacy overload: preserves old behavior for existing callers
     * (APICreateVmInstanceMsg, APIAttachL3NetworkToVmMsg).
     * Uses default NicRoleContext(false, false) and empty ExistingIpContext.
     */
    public List<String> fillUpStaticIpInfoToVmNics(Map<String, NicIpAddressInfo> staticIps) {
        return fillUpStaticIpInfoToVmNics(staticIps,
                new NicRoleContext(false, false), new ExistingIpContext());
    }

    public void validateSystemTagInApiMessage(APIMessage msg) {
        Map<String, NicIpAddressInfo> staticIps = getNicNetworkInfoBySystemTag(msg.getSystemTags());
        validateIpAvailability(staticIps);
        List<String> newSystags = fillUpStaticIpInfoToVmNics(staticIps);
        if (!newSystags.isEmpty()) {
            if (msg.getSystemTags() != null) {
                // Remove any existing netmask/gateway/prefix tags before adding resolved ones
                msg.getSystemTags().removeIf(tag ->
                        VmSystemTags.IPV4_NETMASK.isMatch(tag) || VmSystemTags.IPV4_GATEWAY.isMatch(tag)
                                || VmSystemTags.IPV6_PREFIX.isMatch(tag) || VmSystemTags.IPV6_GATEWAY.isMatch(tag));
            }
            msg.getSystemTags().addAll(newSystags);
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
