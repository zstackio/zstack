package org.zstack.core;

import org.apache.commons.lang.StringUtils;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.zsha2.ZSha2Info;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ManagementNodeAddressInventory {
    private final Map<Integer, String> currentNodeAddresses = new HashMap<>();
    private final Map<Integer, String> haNodeAddresses = new HashMap<>();
    private final Map<Integer, String> haVirtualAddresses = new HashMap<>();
    private final Set<Integer> nestedHaFamilies = new HashSet<>();
    private final boolean haEnabled;

    public ManagementNodeAddressInventory(Collection<String> currentNodeAddresses) {
        this(currentNodeAddresses, null);
    }

    public ManagementNodeAddressInventory(Collection<String> currentNodeAddresses, ZSha2Info zsha2Info) {
        haEnabled = zsha2Info != null;
        if (currentNodeAddresses != null) {
            currentNodeAddresses.forEach(address -> put(this.currentNodeAddresses, address));
        }

        if (zsha2Info == null) {
            return;
        }

        addHaFamily(IPv6Constants.IPv4, zsha2Info.getIpv4());
        addHaFamily(IPv6Constants.IPv6, zsha2Info.getIpv6());
        if (nestedHaFamilies.isEmpty()) {
            addLegacyHaAddress(haNodeAddresses, zsha2Info.getNodeip());
            addLegacyHaAddress(haVirtualAddresses, zsha2Info.getDbvip());
        }
    }

    public String getPrimaryCurrentNodeAddress() {
        String ipv4 = currentNodeAddresses.get(IPv6Constants.IPv4);
        return ipv4 != null ? ipv4 : currentNodeAddresses.get(IPv6Constants.IPv6);
    }

    public Optional<String> findCurrentNodeAddress(int ipVersion) {
        return Optional.ofNullable(currentNodeAddresses.get(ipVersion));
    }

    public Optional<String> findHaNodeAddress(int ipVersion) {
        return Optional.ofNullable(haNodeAddresses.get(ipVersion));
    }

    public Optional<String> findHaVirtualAddress(int ipVersion) {
        return Optional.ofNullable(haVirtualAddresses.get(ipVersion));
    }

    public boolean isHaEnabled() {
        return haEnabled;
    }

    static Integer addressFamilyOf(String address) {
        if (StringUtils.isBlank(address)) {
            return null;
        }

        String normalized = normalize(address);
        if (NetworkUtils.isIpv4Address(normalized)) {
            return IPv6Constants.IPv4;
        }
        if (IPv6NetworkUtils.isIpv6Address(normalized)) {
            return IPv6Constants.IPv6;
        }
        return null;
    }

    static String normalize(String address) {
        String normalized = address.trim();
        return IPv6NetworkUtils.isIpv6Address(normalized)
                ? IPv6NetworkUtils.normalizeIpv6(normalized)
                : normalized;
    }

    private void addHaFamily(int expectedFamily, ZSha2Info.HaAddressFamily family) {
        if (family == null) {
            return;
        }

        nestedHaFamilies.add(expectedFamily);
        if (!family.isEnabled()
                || !hasFamily(family.getNodeIp(), expectedFamily)
                || !hasFamily(family.getPeerIp(), expectedFamily)
                || !hasFamily(family.getVirtualIp(), expectedFamily)) {
            return;
        }

        haNodeAddresses.put(expectedFamily, normalize(family.getNodeIp()));
        haVirtualAddresses.put(expectedFamily, normalize(family.getVirtualIp()));
    }

    private void addLegacyHaAddress(Map<Integer, String> addresses, String address) {
        Integer family = addressFamilyOf(address);
        if (family != null) {
            addresses.putIfAbsent(family, normalize(address));
        }
    }

    private static void put(Map<Integer, String> addresses, String address) {
        Integer family = addressFamilyOf(address);
        if (family != null) {
            addresses.putIfAbsent(family, normalize(address));
        }
    }

    private static boolean hasFamily(String address, int expectedFamily) {
        Integer actualFamily = addressFamilyOf(address);
        return actualFamily != null && actualFamily == expectedFamily;
    }
}
