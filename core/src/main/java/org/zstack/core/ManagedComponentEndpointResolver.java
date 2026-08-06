package org.zstack.core;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10001;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10003;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10004;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10005;

public class ManagedComponentEndpointResolver {
    @FunctionalInterface
    public interface HostAddressResolver {
        Collection<String> resolve(String hostname) throws UnknownHostException;
    }

    private final ManagementNodeAddressInventory managementNodeAddresses;
    private final HostAddressResolver hostAddressResolver;

    public ManagedComponentEndpointResolver(ManagementNodeAddressInventory managementNodeAddresses) {
        this(managementNodeAddresses, hostname -> Arrays.stream(InetAddress.getAllByName(hostname))
                .map(InetAddress::getHostAddress)
                .collect(Collectors.toList()));
    }

    public ManagedComponentEndpointResolver(ManagementNodeAddressInventory managementNodeAddresses,
                                            HostAddressResolver hostAddressResolver) {
        this.managementNodeAddresses = managementNodeAddresses;
        this.hostAddressResolver = hostAddressResolver;
    }

    public ErrorableValue<List<ManagedComponentEndpoint>> resolve(String remoteHost) {
        String normalizedHost = normalizeRemoteAddress(remoteHost);
        if (StringUtils.isBlank(normalizedHost)) {
            return ErrorableValue.ofErrorCode(Platform.argerr(ORG_ZSTACK_CORE_PLATFORM_10003,
                    "cannot resolve an empty remote endpoint"));
        }

        if (NetworkUtils.isIpAddress(normalizedHost)) {
            return resolve(normalizedHost, Collections.singletonList(normalizedHost));
        }

        try {
            return resolve(normalizedHost, hostAddressResolver.resolve(normalizedHost));
        } catch (UnknownHostException e) {
            return ErrorableValue.ofErrorCode(Platform.operr(ORG_ZSTACK_CORE_PLATFORM_10004,
                    "cannot resolve remote endpoint[%s]: %s", normalizedHost, e.getMessage()));
        }
    }

    public ErrorableValue<List<ManagedComponentEndpoint>> resolve(String remoteHost,
                                                                   Collection<String> resolvedAddresses) {
        String normalizedHost = normalizeRemoteAddress(remoteHost);
        List<ManagedComponentEndpoint> endpoints = new ArrayList<>();
        Set<String> uniqueAddresses = new LinkedHashSet<>();
        Set<String> missingFamilies = new LinkedHashSet<>();

        if (resolvedAddresses != null) {
            for (String address : resolvedAddresses) {
                String remoteAddress = normalizeRemoteAddress(address);
                Integer family = ManagementNodeAddressInventory.addressFamilyOf(remoteAddress);
                if (family == null || !uniqueAddresses.add(remoteAddress)) {
                    continue;
                }

                String currentNodeAddress = managementNodeAddresses.findCurrentNodeAddress(family).orElse(null);
                if (currentNodeAddress == null) {
                    missingFamilies.add(familyName(family));
                } else {
                    endpoints.add(new ManagedComponentEndpoint(remoteAddress, currentNodeAddress));
                }
            }
        }

        if (!endpoints.isEmpty()) {
            return ErrorableValue.of(endpoints);
        }

        if (!missingFamilies.isEmpty()) {
            return ErrorableValue.ofErrorCode(Platform.operr(ORG_ZSTACK_CORE_PLATFORM_10001,
                    "cannot resolve remote endpoint[%s]: resolved address families[%s] have no configured current management node address; HA enabled[%s]",
                    normalizedHost, String.join(", ", missingFamilies), managementNodeAddresses.isHaEnabled()));
        }

        return ErrorableValue.ofErrorCode(Platform.argerr(ORG_ZSTACK_CORE_PLATFORM_10005,
                "remote endpoint[%s] has no valid IPv4 or IPv6 address", normalizedHost));
    }

    private static String normalizeRemoteAddress(String address) {
        String stripped = IPv6NetworkUtils.stripHostUrlBrackets(address == null ? null : address.trim());
        return StringUtils.isBlank(stripped) ? stripped : ManagementNodeAddressInventory.normalize(stripped);
    }

    private static String familyName(int family) {
        return family == IPv6Constants.IPv4 ? "IPv4" : "IPv6";
    }
}
