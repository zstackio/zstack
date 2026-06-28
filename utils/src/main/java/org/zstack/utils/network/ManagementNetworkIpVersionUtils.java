package org.zstack.utils.network;

import org.apache.commons.lang.StringUtils;

public class ManagementNetworkIpVersionUtils {
    public static final String IPV4 = "ipv4";
    public static final String IPV6 = "ipv6";

    private static final String IPV6_HOST_PREFIX = "[";
    private static final String IPV6_HOST_SUFFIX = "]";
    private static final String URI_SCHEME_SEPARATOR = "://";
    private static final String NFS_URL_SEPARATOR = ":/";
    private static final String PORT_SEPARATOR = ":";
    private static final String PATH_SEPARATOR = "/";
    private static final String USER_INFO_SEPARATOR = "@";
    private static final String IPV6_SCOPE_SEPARATOR = "%";

    public static boolean isValidIpVersion(String ipVersion) {
        return normalizeIpVersion(ipVersion) != null;
    }

    public static String normalizeIpVersion(String ipVersion) {
        if (StringUtils.isBlank(ipVersion)) {
            return null;
        }

        String normalizedIpVersion = ipVersion.trim().toLowerCase();
        if (IPV4.equals(normalizedIpVersion) || IPV6.equals(normalizedIpVersion)) {
            return normalizedIpVersion;
        }

        return null;
    }

    public static String getEndpointIpVersion(String endpoint) {
        String host = extractEndpointHost(endpoint);
        if (NetworkUtils.isIpv4Address(host)) {
            return IPV4;
        }

        if (IPv6NetworkUtils.isIpv6Address(host)) {
            return IPV6;
        }

        return null;
    }

    public static boolean isIpv6LinkLocalEndpoint(String endpoint) {
        String host = extractEndpointHost(endpoint);
        if (StringUtils.isBlank(host) || !host.contains(PORT_SEPARATOR)) {
            return false;
        }

        int scopeIndex = host.indexOf(IPV6_SCOPE_SEPARATOR);
        String address = scopeIndex >= 0 ? host.substring(0, scopeIndex) : host;
        return IPv6NetworkUtils.isLinkLocalAddress(address);
    }

    public static String extractEndpointHost(String endpoint) {
        if (StringUtils.isBlank(endpoint)) {
            return null;
        }

        String value = endpoint.trim();
        int schemeIndex = value.indexOf(URI_SCHEME_SEPARATOR);
        if (schemeIndex >= 0) {
            value = value.substring(schemeIndex + URI_SCHEME_SEPARATOR.length());
        }

        if (value.contains(USER_INFO_SEPARATOR)) {
            value = value.substring(value.lastIndexOf(USER_INFO_SEPARATOR) + USER_INFO_SEPARATOR.length());
        }

        if (value.startsWith(IPV6_HOST_PREFIX)) {
            return extractBracketIpv6Host(value);
        }

        String hostBeforePath = value;
        int pathIndex = hostBeforePath.indexOf(PATH_SEPARATOR);
        if (pathIndex >= 0) {
            hostBeforePath = hostBeforePath.substring(0, pathIndex);
        }

        if (IPv6NetworkUtils.isIpv6Address(hostBeforePath)) {
            return hostBeforePath;
        }

        int nfsIndex = value.indexOf(NFS_URL_SEPARATOR);
        if (nfsIndex >= 0) {
            return value.substring(0, nfsIndex);
        }

        int portIndex = hostBeforePath.indexOf(PORT_SEPARATOR);
        if (portIndex >= 0 && hostBeforePath.indexOf(PORT_SEPARATOR, portIndex + PORT_SEPARATOR.length()) < 0) {
            return hostBeforePath.substring(0, portIndex);
        }

        return hostBeforePath;
    }

    private static String extractBracketIpv6Host(String value) {
        int end = value.indexOf(IPV6_HOST_SUFFIX);
        return end > 0 ? value.substring(IPV6_HOST_PREFIX.length(), end) : value;
    }
}
