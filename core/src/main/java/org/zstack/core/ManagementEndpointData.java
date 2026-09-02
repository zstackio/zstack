package org.zstack.core;

import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.zsha2.ZSha2Info;

import java.util.Collection;
import java.util.Optional;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10000;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10001;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLATFORM_10002;

@Deprecated
public class ManagementEndpointData {
    public enum EndpointType {
        NODE,
        CANONICAL_NODE,
        VIP
    }

    private final ManagementNodeAddressInventory inventory;

    public ManagementEndpointData(Collection<String> nodeIps) {
        inventory = new ManagementNodeAddressInventory(nodeIps);
    }

    public ManagementEndpointData(Collection<String> nodeIps, ZSha2Info info) {
        inventory = new ManagementNodeAddressInventory(nodeIps, info);
    }

    public ErrorableValue<String> selectForTarget(EndpointType endpointType, String targetIp) {
        Integer family = ManagementNodeAddressInventory.addressFamilyOf(targetIp);
        if (family == null) {
            return ErrorableValue.ofErrorCode(Platform.argerr(ORG_ZSTACK_CORE_PLATFORM_10000,
                    "cannot select management %s endpoint because target[%s] is not an IPv4 or IPv6 address",
                    endpointName(endpointType), targetIp));
        }

        String endpoint = findEndpoint(endpointType, family).orElse(null);
        if (endpoint != null) {
            return ErrorableValue.of(endpoint);
        }

        if (inventory.isHaEnabled() && endpointType != EndpointType.NODE) {
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

        if (inventory.isHaEnabled() && endpointType != EndpointType.NODE) {
            return ErrorableValue.ofErrorCode(Platform.operr(ORG_ZSTACK_CORE_PLATFORM_10002,
                    "cannot select default management %s endpoint: HA default family record is missing or invalid",
                    endpointName(endpointType)));
        }

        return ErrorableValue.ofErrorCode(Platform.operr(ORG_ZSTACK_CORE_PLATFORM_10001,
                "cannot select default management %s endpoint because no configured endpoint exists",
                endpointName(endpointType)));
    }

    public String getDefaultEndpoint(EndpointType endpointType) {
        String currentNodeAddress = inventory.getPrimaryCurrentNodeAddress();
        Integer family = ManagementNodeAddressInventory.addressFamilyOf(currentNodeAddress);
        return family == null ? null : findEndpoint(endpointType, family).orElse(null);
    }

    private Optional<String> findEndpoint(EndpointType endpointType, int family) {
        if (endpointType == EndpointType.NODE || !inventory.isHaEnabled()) {
            return inventory.findCurrentNodeAddress(family);
        }
        return endpointType == EndpointType.CANONICAL_NODE
                ? inventory.findHaNodeAddress(family)
                : inventory.findHaVirtualAddress(family);
    }

    private static String endpointName(EndpointType endpointType) {
        return endpointType.name().toLowerCase().replace('_', ' ');
    }

    private static String familyName(int family) {
        return family == IPv6Constants.IPv4 ? "IPv4" : "IPv6";
    }
}
