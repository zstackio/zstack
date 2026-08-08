package org.zstack.test.integration.core

import org.junit.Test
import org.zstack.core.ManagementEndpointData
import org.zstack.core.ManagementNodeAddressInventory
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.zsha2.ZSha2Info

class ZSha2ManagementEndpointTest {
    private static final String IPV4 = "192.168.1.10"
    private static final String IPV6 = "2001:db8::1"
    private static final String IPV4_TARGET = "192.168.1.20"
    private static final String IPV6_TARGET = "2001:db8::20"

    @Test
    void testNestedFamilyRecordsOverrideLegacyProjectionWithoutCrossFamilyFallback() {
        ZSha2Info info = JSONObjectUtil.toObject('''
            {
              "nodeip": "192.168.1.11",
              "dbvip": "192.168.1.100",
              "ipv4": {"enabled": true, "nodeIp": "192.168.1.11", "peerIp": "192.168.1.12", "virtualIp": "192.168.1.100"},
              "ipv6": {"enabled": true, "nodeIp": "2001:db8::11", "peerIp": "2001:db8::12", "virtualIp": "2001:db8::100"}
            }
        ''', ZSha2Info.class)

        ManagementEndpointData endpoints = new ManagementEndpointData([IPV4, IPV6], info)
        assert endpoints.selectForTarget(ManagementEndpointData.EndpointType.NODE, IPV4_TARGET).result == IPV4
        assert endpoints.selectForTarget(ManagementEndpointData.EndpointType.CANONICAL_NODE, IPV4_TARGET).result == "192.168.1.11"
        assert endpoints.selectForTarget(ManagementEndpointData.EndpointType.VIP, IPV4_TARGET).result == "192.168.1.100"
        assert endpoints.selectForTarget(ManagementEndpointData.EndpointType.NODE, IPV6_TARGET).result == IPV6
        assert endpoints.selectForTarget(ManagementEndpointData.EndpointType.CANONICAL_NODE, IPV6_TARGET).result == "2001:db8::11"
        assert endpoints.selectForTarget(ManagementEndpointData.EndpointType.VIP, IPV6_TARGET).result == "2001:db8::100"

        ZSha2Info missingIpv6 = JSONObjectUtil.toObject('''
            {
              "nodeip": "192.168.1.11",
              "dbvip": "2001:db8::200",
              "ipv4": {"enabled": true, "nodeIp": "192.168.1.11", "peerIp": "192.168.1.12", "virtualIp": "192.168.1.100"}
            }
        ''', ZSha2Info.class)

        assert !new ManagementEndpointData([IPV4, IPV6], missingIpv6)
                .selectForTarget(ManagementEndpointData.EndpointType.CANONICAL_NODE, IPV6_TARGET).success
        assert !new ManagementEndpointData([IPV4, IPV6], missingIpv6)
                .selectForTarget(ManagementEndpointData.EndpointType.VIP, IPV6_TARGET).success

        ZSha2Info legacyIpv4 = new ZSha2Info(nodeip: "192.168.1.11", dbvip: "192.168.1.100")
        ManagementEndpointData legacy = new ManagementEndpointData([IPV4], legacyIpv4)
        assert legacy.selectForTarget(ManagementEndpointData.EndpointType.CANONICAL_NODE, "192.168.1.20").result == "192.168.1.11"
        assert legacy.selectForTarget(ManagementEndpointData.EndpointType.VIP, "192.168.1.20").result == "192.168.1.100"
    }

    @Test
    void testInventoryExposesAddressRolesWithoutGenericEndpointTypes() {
        ZSha2Info info = JSONObjectUtil.toObject('''
            {
              "nodeip": "2001:db8::11",
              "dbvip": "192.168.1.100",
              "ipv4": {"enabled": true, "nodeIp": "192.168.1.11", "peerIp": "192.168.1.12", "virtualIp": "192.168.1.100"},
              "ipv6": {"enabled": true, "nodeIp": "2001:db8::11", "peerIp": "2001:db8::12", "virtualIp": "2001:db8::100"}
            }
        ''', ZSha2Info.class)

        ManagementNodeAddressInventory inventory = new ManagementNodeAddressInventory([IPV6, IPV4], info)

        assert inventory.primaryCurrentNodeAddress == IPV4
        assert inventory.findCurrentNodeAddress(IPv6Constants.IPv4).orElse(null) == IPV4
        assert inventory.findCurrentNodeAddress(IPv6Constants.IPv6).orElse(null) == IPV6
        assert inventory.findHaNodeAddress(IPv6Constants.IPv4).orElse(null) == "192.168.1.11"
        assert inventory.findHaNodeAddress(IPv6Constants.IPv6).orElse(null) == "2001:db8::11"
        assert inventory.findHaVirtualAddress(IPv6Constants.IPv4).orElse(null) == "192.168.1.100"
        assert inventory.findHaVirtualAddress(IPv6Constants.IPv6).orElse(null) == "2001:db8::100"
        assert inventory.haEnabled
    }

    @Test
    void testInventoryDoesNotExposeHaAddressesOutsideHa() {
        ManagementNodeAddressInventory inventory = new ManagementNodeAddressInventory([IPV6])

        assert inventory.primaryCurrentNodeAddress == IPV6
        assert inventory.findCurrentNodeAddress(IPv6Constants.IPv6).orElse(null) == IPV6
        assert !inventory.findHaNodeAddress(IPv6Constants.IPv6).present
        assert !inventory.findHaVirtualAddress(IPv6Constants.IPv6).present
        assert !inventory.haEnabled
    }
}
