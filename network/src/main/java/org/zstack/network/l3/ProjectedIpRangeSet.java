package org.zstack.network.l3;

import com.googlecode.ipv6.IPv6Address;
import org.apache.commons.net.util.SubnetUtils;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.AddressPoolVO;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public final class ProjectedIpRangeSet {
    public static class Rejected extends IllegalArgumentException {
        private final String globalErrorCode;

        Rejected(String globalErrorCode, String message) {
            super(message);
            this.globalErrorCode = globalErrorCode;
        }

        public String getGlobalErrorCode() { return globalErrorCode; }
    }

    private final List<IpRangeInventory> ranges = new ArrayList<>();
    private final List<String> deletedRangeUuids = new ArrayList<>();
    private final Map<String, IpRangeInventory> usedIpTargets = new LinkedHashMap<>();
    private final List<String> deletedUsedIpUuids = new ArrayList<>();

    public List<IpRangeInventory> getRanges() { return ranges; }
    public List<String> getDeletedRangeUuids() { return deletedRangeUuids; }
    public Map<String, IpRangeInventory> getUsedIpTargets() { return usedIpTargets; }
    public List<String> getDeletedUsedIpUuids() { return deletedUsedIpUuids; }

    public void validateUnmanagedRanges(String l3Uuid, List<IpRangeVO> protectedRanges) {
        for (IpRangeVO existing : protectedRanges) {
            for (IpRangeInventory target : ranges) {
                if (existing.getIpVersion() == target.getIpVersion()
                        && (NetworkUtils.isInRange(target.getStartIp(), existing.getStartIp(), existing.getEndIp())
                        || NetworkUtils.isInRange(existing.getStartIp(), target.getStartIp(), target.getEndIp()))) {
                    throw new Rejected(ORG_ZSTACK_NETWORK_L3_10114,
                            "projected IP range overlaps unmanaged IP range[uuid:" + existing.getUuid() + "]");
                }
            }
            if (existing instanceof AddressPoolVO && l3Uuid.equals(existing.getL3NetworkUuid())
                    && existing.getIpVersion() == IPv6Constants.IPv4
                    && ranges.stream().noneMatch(range -> range.getIpVersion() == IPv6Constants.IPv4)) {
                throw new Rejected(ORG_ZSTACK_NETWORK_L3_10115,
                        "address pool[uuid:" + existing.getUuid() + "] requires an ordinary IPv4 range");
            }
        }
    }

    public static ProjectedIpRangeSet plan(String l3Uuid, List<NormalIpRangeVO> current,
                                           List<? extends IpRangeInventory> targets,
                                           List<UsedIpVO> usedIps, String dhcpServerIpUuid) {
        return plan(l3Uuid, current, targets, usedIps, dhcpServerIpUuid, null);
    }

    public static ProjectedIpRangeSet plan(String l3Uuid, List<NormalIpRangeVO> current,
                                           List<? extends IpRangeInventory> targets,
                                           List<UsedIpVO> usedIps, String dhcpServerIpUuid,
                                           String obsoleteDhcpServerIpUuid) {
        if (targets == null) {
            throw new Rejected(ORG_ZSTACK_NETWORK_L3_10105, "projected IP range collection is missing");
        }
        ProjectedIpRangeSet plan = new ProjectedIpRangeSet();
        if (obsoleteDhcpServerIpUuid != null) {
            UsedIpVO obsolete = usedIps.stream().filter(ip -> obsoleteDhcpServerIpUuid.equals(ip.getUuid()))
                    .findFirst().orElse(null);
            if (obsolete == null || !l3Uuid.equals(obsolete.getL3NetworkUuid())
                    || !Objects.equals(obsolete.getIpVersion(), IPv6Constants.IPv4)
                    || obsolete.getVmNicUuid() != null || obsolete.getUsedFor() != null
                    || obsoleteDhcpServerIpUuid.equals(dhcpServerIpUuid)) {
                throw new Rejected(ORG_ZSTACK_NETWORK_L3_10116,
                        "obsolete DHCP reservation[uuid:" + obsoleteDhcpServerIpUuid
                                + "] is not a releasable IPv4 reservation on L3 network[uuid:" + l3Uuid + "]");
            }
            plan.deletedUsedIpUuids.add(obsoleteDhcpServerIpUuid);
        }
        Map<String, NormalIpRangeVO> oldByValue = new LinkedHashMap<>();
        for (NormalIpRangeVO old : current) {
            String key = key(old.getIpVersion(), old.getStartIp(), old.getEndIp());
            if (oldByValue.put(key, old) != null) {
                throw new Rejected(ORG_ZSTACK_NETWORK_L3_10106,
                        "multiple Cloud IP ranges have the same bounds on L3 network[uuid:" + l3Uuid + "]");
            }
        }
        Map<Integer, String> familySubnets = new HashMap<>();
        for (IpRangeInventory input : targets) {
            IpRangeInventory target = normalize(input);
            String subnet = target.getNetworkCidr() + ":" + target.getGateway();
            String previousSubnet = familySubnets.put(target.getIpVersion(), subnet);
            if (previousSubnet != null && !previousSubnet.equals(subnet)) {
                throw new Rejected(ORG_ZSTACK_NETWORK_L3_10107,
                        "projected IP ranges contain conflicting subnet metadata for IP version " + target.getIpVersion());
            }
            for (IpRangeInventory previous : plan.ranges) {
                if (previous.getIpVersion() == target.getIpVersion()
                        && (NetworkUtils.isInRange(target.getStartIp(), previous.getStartIp(), previous.getEndIp())
                        || NetworkUtils.isInRange(previous.getStartIp(), target.getStartIp(), target.getEndIp()))) {
                    throw new Rejected(ORG_ZSTACK_NETWORK_L3_10108,
                            "projected IP ranges overlap at address " + target.getStartIp());
                }
            }
            NormalIpRangeVO old = oldByValue.get(key(target.getIpVersion(), target.getStartIp(), target.getEndIp()));
            target.setL3NetworkUuid(l3Uuid);
            if (old == null) {
                target.setUuid(UUID.randomUUID().toString().replace("-", ""));
                target.setName(input.getName() == null ? target.getStartIp() + "-" + target.getEndIp() : input.getName());
            } else {
                target.setUuid(old.getUuid());
                target.setName(old.getName());
                target.setDescription(old.getDescription());
                target.setCreateDate(old.getCreateDate());
            }
            plan.ranges.add(target);
        }
        Set<String> remaining = plan.ranges.stream().map(IpRangeInventory::getUuid).collect(Collectors.toSet());
        Set<String> oldUuids = current.stream().map(NormalIpRangeVO::getUuid).collect(Collectors.toSet());
        current.stream().filter(old -> !remaining.contains(old.getUuid()))
                .forEach(old -> plan.deletedRangeUuids.add(old.getUuid()));
        for (UsedIpVO used : usedIps) {
            if (plan.deletedUsedIpUuids.contains(used.getUuid())) {
                continue;
            }
            boolean dhcp = Objects.equals(dhcpServerIpUuid, used.getUuid())
                    && used.getIpVersion() == IPv6Constants.IPv4
                    && used.getVmNicUuid() == null && used.getUsedFor() == null;
            if (!oldUuids.contains(used.getIpRangeUuid()) && !(dhcp && used.getIpRangeUuid() == null)) {
                continue;
            }
            IpRangeInventory target = plan.ranges.stream()
                    .filter(range -> Objects.equals(range.getIpVersion(), used.getIpVersion()))
                    .filter(range -> NetworkUtils.isInRange(used.getIp(), range.getStartIp(), range.getEndIp()))
                    .findFirst().orElse(null);
            if (target == null) {
                boolean preserveDhcp = dhcp && plan.ranges.stream()
                        .filter(range -> range.getIpVersion() == IPv6Constants.IPv4)
                        .anyMatch(range -> new SubnetUtils(range.getNetworkCidr()).getInfo().isInRange(used.getIp()));
                if (!preserveDhcp) {
                    throw new Rejected(ORG_ZSTACK_NETWORK_L3_10109,
                            "used IP[uuid:" + used.getUuid() + ", address:" + used.getIp()
                                    + "] has no target in the projected range set");
                }
            }
            plan.usedIpTargets.put(used.getUuid(), target);
        }
        return plan;
    }

    private static IpRangeInventory normalize(IpRangeInventory input) {
        try {
            IpRangeInventory range = new IpRangeInventory();
            int family = input.getIpVersion();
            range.setIpVersion(family);
            range.setAddressMode(input.getAddressMode());
            if (family == IPv6Constants.IPv4) {
                if (!NetworkUtils.isIpv4Address(input.getStartIp()) || !NetworkUtils.isIpv4Address(input.getEndIp())
                        || !NetworkUtils.isIpv4Address(input.getGateway())
                        || !NetworkUtils.isNetmaskExcept(input.getNetmask(), "0.0.0.0")) {
                    throw new IllegalArgumentException();
                }
                SubnetUtils.SubnetInfo subnet = new SubnetUtils(input.getStartIp(), input.getNetmask()).getInfo();
                if (!subnet.isInRange(input.getStartIp()) || !subnet.isInRange(input.getEndIp())
                        || !subnet.isInRange(input.getGateway())) {
                    throw new IllegalArgumentException();
                }
                range.setStartIp(NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(input.getStartIp())));
                range.setEndIp(NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(input.getEndIp())));
                range.setGateway(NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(input.getGateway())));
                range.setNetmask(input.getNetmask());
                range.setPrefixLen(NetworkUtils.getPrefixLengthFromNetmask(input.getNetmask()));
                range.setNetworkCidr(subnet.getNetworkAddress() + "/" + range.getPrefixLen());
            } else if (family == IPv6Constants.IPv6) {
                int prefix = input.getPrefixLen() == null
                        ? IPv6NetworkUtils.getPrefixLengthFromNetmask(input.getNetmask()) : input.getPrefixLen();
                if (prefix < IPv6Constants.IPV6_PREFIX_LEN_MIN || prefix > IPv6Constants.IPV6_PREFIX_LEN_MAX
                        || !IPv6NetworkUtils.isValidUnicastIpv6Range(input.getStartIp(), input.getEndIp(),
                        input.getGateway(), prefix)) {
                    throw new IllegalArgumentException();
                }
                range.setStartIp(IPv6Address.fromString(input.getStartIp()).toString());
                range.setEndIp(IPv6Address.fromString(input.getEndIp()).toString());
                range.setGateway(IPv6Address.fromString(input.getGateway()).toString());
                range.setNetmask(IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(range.getGateway() + "/" + prefix));
                range.setPrefixLen(prefix);
                range.setNetworkCidr(IPv6NetworkUtils.getNetworkCidrOfIpRange(range.getStartIp(), prefix));
            } else {
                throw new IllegalArgumentException();
            }
            if (!NetworkUtils.isInRange(range.getStartIp(), range.getStartIp(), range.getEndIp())
                    || NetworkUtils.isInRange(range.getGateway(), range.getStartIp(), range.getEndIp())) {
                throw new IllegalArgumentException();
            }
            return range;
        } catch (RuntimeException error) {
            throw new Rejected(ORG_ZSTACK_NETWORK_L3_10110, "projected IP range has invalid bounds or subnet metadata");
        }
    }

    private static String key(int version, String start, String end) {
        if (version == IPv6Constants.IPv6) {
            return version + ":" + IPv6Address.fromString(start) + "-" + IPv6Address.fromString(end);
        }
        return version + ":" + NetworkUtils.ipv4StringToLong(start) + "-" + NetworkUtils.ipv4StringToLong(end);
    }
}
