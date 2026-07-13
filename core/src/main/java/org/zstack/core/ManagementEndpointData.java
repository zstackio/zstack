package org.zstack.core;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.zsha2.ZSha2Info;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10000;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10001;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10002;

public class ManagementEndpointData {
    public enum EndpointType {
        NODE,
        CANONICAL_NODE,
        VIP
    }

    private final Map<Integer, String> nodeIps = new HashMap<>();
    private final Map<Integer, String> haNodeIps = new HashMap<>();
    private final Map<Integer, String> haVips = new HashMap<>();
    private final Set<Integer> nestedHaFamilies = new HashSet<>();
    private final boolean ha;

    public ManagementEndpointData(Collection<String> nodeIps) {
        this(nodeIps, null);
    }

    public ManagementEndpointData(Collection<String> nodeIps, ZSha2Info info) {
        this.ha = info != null;
        for (String ip : nodeIps) {
            put(this.nodeIps, ip);
        }
        if (info != null) {
            addHaFamily(IPv6Constants.IPv4, info.getIpv4());
            addHaFamily(IPv6Constants.IPv6, info.getIpv6());
            if (nestedHaFamilies.isEmpty()) {
                addLegacyHaEndpoint(haNodeIps, info.getNodeip());
                addLegacyHaEndpoint(haVips, info.getDbvip());
            }
        }
    }

    public ErrorableValue<String> selectForTarget(EndpointType endpointType, String targetIp) {
        Integer family = getAddressFamily(targetIp);
        if (family == null) {
            return ErrorableValue.ofErrorCode(Platform.argerr(ORG_ZSTACK_CORE_PLATFORM_10000,
                    "cannot select management %s endpoint because target[%s] is not an IPv4 or IPv6 address",
                    endpointName(endpointType), targetIp));
        }

        String endpoint = getEndpoint(endpointType, family);
        if (endpoint != null) {
            return ErrorableValue.of(endpoint);
        }

        if (ha && endpointType != EndpointType.NODE) {
            return ErrorableValue.ofErrorCode(Platform.operr(ORG_ZSTACK_CORE_PLATFORM_10002,
                    "cannot select management %s endpoint for %s target[%s]: HA %s family record is missing or invalid",
                    endpointName(endpointType), familyName(family), targetIp, familyName(family)));
        }

        return ErrorableValue.ofErrorCode(Platform.operr(ORG_ZSTACK_CORE_PLATFORM_10001,
                "cannot select management %s endpoint for %s target[%s]: no configured %s endpoint exists",
                endpointName(endpointType), familyName(family), targetIp, familyName(family)));
    }

    public ErrorableValue<String> selectDefault(EndpointType endpointType) {
        String endpoint = getDefaultEndpoint(endpointType);
        if (endpoint != null) {
            return ErrorableValue.of(endpoint);
        }

        if (ha && endpointType != EndpointType.NODE) {
            return ErrorableValue.ofErrorCode(Platform.operr(ORG_ZSTACK_CORE_PLATFORM_10002,
                    "cannot select default management %s endpoint: HA default family record is missing or invalid",
                    endpointName(endpointType)));
        }

        return ErrorableValue.ofErrorCode(Platform.operr(ORG_ZSTACK_CORE_PLATFORM_10001,
                "cannot select default management %s endpoint because no configured endpoint exists",
                endpointName(endpointType)));
    }

    public String getDefaultEndpoint(EndpointType endpointType) {
        Integer defaultFamily = null;
        if (nodeIps.containsKey(IPv6Constants.IPv4)) {
            defaultFamily = IPv6Constants.IPv4;
        } else if (nodeIps.containsKey(IPv6Constants.IPv6)) {
            defaultFamily = IPv6Constants.IPv6;
        }
        return defaultFamily == null ? null : getEndpoint(endpointType, defaultFamily);
    }

    private String getEndpoint(EndpointType endpointType, int family) {
        if (endpointType == EndpointType.NODE || !ha) {
            return nodeIps.get(family);
        }
        return endpointType == EndpointType.CANONICAL_NODE ? haNodeIps.get(family) : haVips.get(family);
    }

    private void addHaFamily(int expectedFamily, ZSha2Info.HaAddressFamily family) {
        if (family == null) {
            return;
        }
        nestedHaFamilies.add(expectedFamily);
        if (!family.isEnabled() || !hasFamily(family.getNodeIp(), expectedFamily)
                || !hasFamily(family.getPeerIp(), expectedFamily)
                || !hasFamily(family.getVirtualIp(), expectedFamily)) {
            return;
        }
        haNodeIps.put(expectedFamily, normalize(family.getNodeIp()));
        haVips.put(expectedFamily, normalize(family.getVirtualIp()));
    }

    private void addLegacyHaEndpoint(Map<Integer, String> endpoints, String ip) {
        Integer family = getAddressFamily(ip);
        if (family != null && !endpoints.containsKey(family)) {
            endpoints.put(family, normalize(ip));
        }
    }

    private static void put(Map<Integer, String> endpoints, String ip) {
        Integer family = getAddressFamily(ip);
        if (family != null) {
            endpoints.putIfAbsent(family, normalize(ip));
        }
    }

    private static boolean hasFamily(String ip, int expectedFamily) {
        Integer actualFamily = getAddressFamily(ip);
        return actualFamily != null && actualFamily == expectedFamily;
    }

    private static Integer getAddressFamily(String ip) {
        if (StringUtils.isBlank(ip)) {
            return null;
        }

        String normalized = normalize(ip);
        if (NetworkUtils.isIpv4Address(normalized)) {
            return IPv6Constants.IPv4;
        }
        if (IPv6NetworkUtils.isIpv6Address(normalized)) {
            return IPv6Constants.IPv6;
        }
        return null;
    }

    private static String normalize(String ip) {
        String normalized = ip.trim();
        return IPv6NetworkUtils.isIpv6Address(normalized) ? IPv6NetworkUtils.normalizeIpv6(normalized) : normalized;
    }

    private static String endpointName(EndpointType endpointType) {
        return endpointType.name().toLowerCase().replace('_', ' ');
    }

    private static String familyName(int family) {
        return family == IPv6Constants.IPv4 ? "IPv4" : "IPv6";
    }
}
