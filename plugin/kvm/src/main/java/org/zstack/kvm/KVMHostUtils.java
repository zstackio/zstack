package org.zstack.kvm;

import org.apache.commons.codec.digest.DigestUtils;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagType;
import org.zstack.utils.TagUtils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.ssh.SshResult;
import org.zstack.utils.ssh.SshShell;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by GuoYi on 4/16/20.
 */
public class KVMHostUtils {
    private static final CLogger logger = CLoggerImpl.getLogger(KVMHostUtils.class);

    /**
     * Collect host IPv4 addresses, mirroring zstack-utility host_plugin.fact()
     * (only filter: ifname endswith "zs", drop 127.0.0.1 / MN VIP).
     */
    public static String collectHostIps(SshShell sshShell, String hostUuid, String managementIp) {
        if (sshShell == null) {
            return managementIp;
        }
        // Run a quote-free command; parse on the MN side to avoid nested-quote
        // mangling when SshShell wraps the command with single quotes.
        SshResult r = sshShell.runCommand("ip -4 -o addr show");
        if (r.isSshFailure() || r.getReturnCode() != 0
                || r.getStdout() == null || r.getStdout().trim().isEmpty()) {
            logger.warn(String.format(
                    "ssh-collect host IPs failed on host[uuid:%s], fallback to mgmtIp: %s",
                    hostUuid, r.getExitErrorMessage()));
            return managementIp;
        }
        return buildIpList(managementIp, r.getStdout(), CoreGlobalProperty.MN_VIP);
    }

    /**
     * TLS cert IP list for ansible deploy: detectedIps ∪ EXTRA_IPS tag.
     * detectedIps comes from the prior check-tls-certs flow (no second SSH).
     * If detectedIps is null/empty, fall back to managementIp + EXTRA_IPS.
     */
    public static String unionTlsCertIps(String hostUuid, String managementIp, String detectedIpsCsv) {
        String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(
                hostUuid, HostSystemTags.EXTRA_IPS_TOKEN);
        return unionIps(detectedIpsCsv, managementIp, extraIps, CoreGlobalProperty.MN_VIP);
    }

    /** Pure function for unit tests: detectedIps ∪ extraIps, fall back to mgmtIp when detected empty. */
    public static String unionIps(String detectedIpsCsv, String managementIp, String extraIpsCsv, String mnVip) {
        Set<String> ips = new LinkedHashSet<>();
        if (detectedIpsCsv != null && !detectedIpsCsv.trim().isEmpty()) {
            for (String ip : detectedIpsCsv.split(",")) {
                String t = ip.trim();
                if (!t.isEmpty()) {
                    ips.add(t);
                }
            }
        } else {
            ips.add(managementIp);
        }

        if (extraIpsCsv != null && !extraIpsCsv.isEmpty()) {
            for (String ip : extraIpsCsv.split(",")) {
                String t = ip.trim();
                if (!t.isEmpty()) {
                    ips.add(t);
                }
            }
        }

        ips.remove("127.0.0.1");
        if (mnVip != null) {
            ips.remove(mnVip);
        }
        return String.join(",", ips);
    }

    /**
     * Parse the raw output of "ip -4 -o addr show" and build the IP list,
     * mirroring zstack-utility host_plugin.fact() (filter ifname endswith "zs",
     * drop 127.0.0.1 / MN VIP). Each line looks like:
     *   "5: zsn0.2000    inet 12.1.251.206/16 brd ... scope global zsn0.2000\..."
     * Pure function for unit tests.
     */
    public static String buildIpList(String managementIp, String ipAddrOutput, String mnVip) {
        Set<String> ips = new LinkedHashSet<>();
        ips.add(managementIp);

        if (ipAddrOutput != null) {
            for (String line : ipAddrOutput.trim().split("\n")) {
                String[] parts = line.trim().split("\\s+");
                // expect at least: "<idx>:" "<iface>" "inet" "<ip>/<prefix>"
                if (parts.length < 4 || !"inet".equals(parts[2])) {
                    continue;
                }
                String iface = parts[1];
                if (iface.endsWith("zs")) {
                    continue;
                }
                String cidr = parts[3];
                int slash = cidr.indexOf('/');
                String ip = slash >= 0 ? cidr.substring(0, slash) : cidr;
                if (!ip.isEmpty()) {
                    ips.add(ip);
                }
            }
        }

        ips.remove("127.0.0.1");
        if (mnVip != null) {
            ips.remove(mnVip);
        }
        return String.join(",", ips);
    }

    public static String collectHostIps(String hostUuid, String managementIp,
                                        String username, String password, int sshPort) {
        return collectHostIps(newSsh(managementIp, username, password, sshPort), hostUuid, managementIp);
    }

    /**
     * ZSTAC-84446: force ansible re-run + libvirtd restart only when operator
     * opted in (RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE) or it's a fresh add
     * (full-deploy will start libvirtd anyway). Skipping on plain reconnect
     * keeps kvmagent PID stable.
     */
    public static boolean shouldForceTlsRedeploy(boolean needDeployTlsCert,
                                                 boolean allowRestartLibvirtd,
                                                 boolean isNewAdded) {
        if (!needDeployTlsCert) {
            return false;
        }
        return allowRestartLibvirtd || isNewAdded;
    }

    private static SshShell newSsh(String host, String user, String pwd, int port) {
        SshShell s = new SshShell();
        s.setHostname(host);
        s.setUsername(user);
        s.setPassword(pwd);
        s.setPort(port);
        return s;
    }

    /**
     * Get normalized bridge name for l2 network, which at most has 15 chars.
     * - if l2 network has L2_BRIDGE_NAME tag, then return it's value directly;
     * - if l2Uuid does not have an L2_BRIDGE_NAME tag and conflict with existing bridge name,
     * use the new naming convention : prefix 'l2_' plus the last 12 characters of l2Uuid;
     * - if l2 physical interface name is short, then no need for truncation;
     * - otherwise, get md5sum of interface name and use the top chars.
     * @param l2Uuid l2 network uuid
     * @param format bridge name format string, like "br_%s", "br_%s_100" (only one '%s' is allowed)
     * @return normalized bridge name, or null if anything wrong
     */
    public static String getNormalizedBridgeName(String l2Uuid, String format) {
        String current = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        if (current != null) {
            return current;
        }

        validateFormatString(format);

        String physicalInterface = getPhysicalInterface(l2Uuid, format);
        String preferredBridgeName = String.format(format, physicalInterface);

        if (!checkNameConflict(l2Uuid, preferredBridgeName)) {
            return preferredBridgeName;
        }

        current = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        if (current != null) {
            return current;
        }

        return generateNewBridgeName(l2Uuid);
    }

    private static void validateFormatString(String format) {
        if (!format.contains("%s") || format.indexOf("%s") != format.lastIndexOf("%s")) {
            throw new IllegalArgumentException(String.format("invalid format string: %s", format));
        }
    }

    private static String generateNewBridgeName(String l2Uuid) {
        return "l2_" + l2Uuid.substring(0, Math.min(l2Uuid.length(), 12));
    }

    private static String getPhysicalInterface(String l2Uuid, String format) {
        String physicalInterface = Q.New(L2NetworkVO.class)
                .eq(L2NetworkVO_.uuid, l2Uuid)
                .select(L2NetworkVO_.physicalInterface)
                .findValue();

        int allowedLen = L2NetworkConstant.LINUX_IF_NAME_MAX_SIZE - format.length() + 2; // "%s" length is 2

        if (physicalInterface != null && physicalInterface.length() > allowedLen) {
            physicalInterface = DigestUtils.md5Hex(physicalInterface).substring(0, allowedLen);
        }
        return physicalInterface;
    }

    public static Boolean checkNameConflict(String l2Uuid, String bridgeName) {
        String pattern = TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.instantiateTag(
                Collections.singletonMap(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, bridgeName)
        ));
        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class)
                .select(L2NetworkClusterRefVO_.clusterUuid)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid).listValues();
        if (clusterUuids.isEmpty()) {
            return Boolean.FALSE;
        }
        Set<String> relatedL2Uuids = new HashSet<>(Q.New(L2NetworkClusterRefVO.class)
                .select(L2NetworkClusterRefVO_.l2NetworkUuid)
                .in(L2NetworkClusterRefVO_.clusterUuid, clusterUuids).listValues());
        List<SystemTagVO> tags = Q.New(SystemTagVO.class)
                .in(SystemTagVO_.resourceUuid, relatedL2Uuids)
                .eq(SystemTagVO_.resourceType, L2NetworkVO.class.getSimpleName())
                .like(SystemTagVO_.tag, pattern)
                .eq(SystemTagVO_.type, TagType.System)
                .list();

        return !tags.isEmpty();
    }
}
