package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.TagUtils;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/5/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StaticIpOperator {
    @Autowired
    private DatabaseFacade dbf;

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

    public Map<String, NetworkUtils.IPAMInfo> getIPAMInfoBySystemTag(List<String> systemTags) {
        Map<String, NetworkUtils.IPAMInfo> ret = new HashMap<>();
        if (systemTags == null) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(VmSystemTags.STATIC_IP.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
                NetworkUtils.IPAMInfo ipamInfo = ret.get(l3Uuid);
                if (ipamInfo == null) {
                    NetworkUtils networkUtils = new NetworkUtils();
                    ret.put(l3Uuid, networkUtils.new IPAMInfo("", "", "",
                            "", "", ""));
                }
                String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
                ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
                if (NetworkUtils.isIpv4Address(ip)) {
                    ret.get(l3Uuid).ipv4Address = ip;
                } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
                    ret.get(l3Uuid).ipv6Address = ip;
                }
            }
            if(VmSystemTags.IPV4_GATEWAY.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN);
                ret.get(l3Uuid).ipv4Gateway = token.get(VmSystemTags.IPV4_GATEWAY_TOKEN);
            }
            if(VmSystemTags.IPV6_GATEWAY.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN);
                ret.get(l3Uuid).ipv6Gateway = IPv6NetworkUtils.ipv6TagValueToAddress(token.get(VmSystemTags.IPV6_GATEWAY_TOKEN));
            }
            if(VmSystemTags.IPV4_NETMASK.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_NETMASK.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN);
                ret.get(l3Uuid).ipv4Netmask = token.get(VmSystemTags.IPV4_NETMASK_TOKEN);
            }
            if(VmSystemTags.IPV6_PREFIX.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_PREFIX.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN);
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
}
