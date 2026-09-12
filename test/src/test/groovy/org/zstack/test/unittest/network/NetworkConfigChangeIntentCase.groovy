package org.zstack.test.unittest.network

import org.junit.Test
import org.zstack.header.network.NetworkConfigChange
import org.zstack.header.network.NetworkConfigChange.CollectionChangeOperation
import org.zstack.header.network.NetworkConfigChange.IpRange
import org.zstack.header.network.l2.NetworkOperationOrigin
import org.zstack.utils.gson.JSONObjectUtil

import static groovy.test.GroovyAssert.shouldFail

class NetworkConfigChangeIntentCase {
    @Test
    void testOldFactoriesPreserveFullReplacementSemantics() {
        def range = new IpRange("range-1", "10.99.44.10", "10.99.44.20")
        def replace = NetworkConfigChange.replaceIpRangeConfiguration("l2", NetworkOperationOrigin.API,
                "op", "account", "l3", 4, "10.99.44.1/24", [range]).ipRangeConfiguration
        assert replace.operation == null
        assert replace.changedRanges.empty
        assert replace.removedRangeUuid == null
        assert !replace.delete
        assert replace.ranges*.rangeUuid == ["range-1"]

        def remove = NetworkConfigChange.removeIpRangeConfiguration("l2", NetworkOperationOrigin.API,
                "op", "account", "l3", 4).ipRangeConfiguration
        assert remove.operation == null
        assert remove.changedRanges.empty
        assert remove.removedRangeUuid == null
        assert remove.delete
        assert remove.ranges.empty

        def dns = NetworkConfigChange.updateDnsConfiguration("l2", NetworkOperationOrigin.API,
                "op", "account", "l3", 4, ["10.99.44.1"]).dhcpDnsConfiguration
        assert dns.operation == null
        assert dns.changedDns == null
        assert dns.dnsServers == ["10.99.44.1"]

        def dhcp = NetworkConfigChange.updateDhcpConfiguration("l2", NetworkOperationOrigin.API,
                "op", "account", "l3", true, ["tag"]).dhcpDnsConfiguration
        assert dhcp.operation == null
        assert dhcp.changedDns == null
        assert dhcp.dnsServers == null
        assert dhcp.ipVersion == null
        assert dhcp.enabled
        assert dhcp.systemTags == ["tag"]
    }

    @Test
    void testDnsIntentSurvivesSerializationWithoutChangingTargetSnapshot() {
        CollectionChangeOperation.values().each { operation ->
            List<String> snapshot = ["10.99.44.1"]
            def change = NetworkConfigChange.updateDnsConfiguration("l2", NetworkOperationOrigin.API,
                    "op", "account", "l3", 4, snapshot, operation, "10.99.44.2")
            snapshot.clear()
            assert change.dhcpDnsConfiguration.dnsServers == ["10.99.44.1"]
            shouldFail(UnsupportedOperationException) {
                change.dhcpDnsConfiguration.dnsServers.add("10.99.44.3")
            }
            def restored = roundTrip(change)
            assert restored.kind == NetworkConfigChange.Kind.DHCP_DNS_CONFIGURATION
            assert restored.l2Uuid == "l2"
            assert restored.operationUuid == "op"
            assert restored.origin == NetworkOperationOrigin.API
            assert restored.dhcpDnsConfiguration.operation == operation
            assert restored.dhcpDnsConfiguration.changedDns == "10.99.44.2"
            assert restored.dhcpDnsConfiguration.dnsServers == ["10.99.44.1"]
        }
    }

    @Test
    void testAddedRangesAreSeparateImmutableIntent() {
        def existing = new IpRange("existing", "10.99.44.10", "10.99.44.20")
        def added = new IpRange("added", "10.99.44.30", "10.99.44.40")
        List<IpRange> ranges = [existing, added]
        List<IpRange> changedRanges = [added]
        def change = NetworkConfigChange.replaceIpRangeConfiguration("l2", NetworkOperationOrigin.API,
                "op", "account", "l3", 4, "10.99.44.1/24", ranges,
                CollectionChangeOperation.ADD, changedRanges, null)
        ranges.clear()
        changedRanges.clear()
        [change, roundTrip(change)].each { value ->
            def configuration = value.ipRangeConfiguration
            assert configuration.operation == CollectionChangeOperation.ADD
            assert configuration.ranges*.rangeUuid == ["existing", "added"]
            assert configuration.changedRanges*.rangeUuid == ["added"]
            assert configuration.changedRanges[0].startIp == "10.99.44.30"
            assert configuration.changedRanges[0].endIp == "10.99.44.40"
            assert configuration.removedRangeUuid == null
            assert configuration.gatewayAddress == "10.99.44.1/24"
            shouldFail(UnsupportedOperationException) { configuration.changedRanges.clear() }
        }
    }

    @Test
    void testRangeRemovalKeepsIdentityWithAndWithoutRemainingRanges() {
        def remaining = new IpRange("remaining", "10.99.44.10", "10.99.44.20")
        def replace = NetworkConfigChange.replaceIpRangeConfiguration("l2", NetworkOperationOrigin.API,
                "op", "account", "l3", 4, "10.99.44.1/24", [remaining],
                CollectionChangeOperation.REMOVE, null, "removed")
        def remove = NetworkConfigChange.removeIpRangeConfiguration("l2", NetworkOperationOrigin.API,
                "op", "account", "l3", 4, "removed")
        [replace, remove].each { change ->
            def restored = roundTrip(change).ipRangeConfiguration
            assert restored.operation == CollectionChangeOperation.REMOVE
            assert restored.removedRangeUuid == "removed"
            assert restored.changedRanges.empty
        }
        assert !roundTrip(replace).ipRangeConfiguration.delete
        assert roundTrip(replace).ipRangeConfiguration.ranges*.rangeUuid == ["remaining"]
        assert roundTrip(remove).ipRangeConfiguration.delete
        assert roundTrip(remove).ipRangeConfiguration.ranges.empty
    }

    @Test
    void testLegacySerializedConfigurationHasNoIntent() {
        String oldIpRange = '''{"kind":"IP_RANGE_CONFIGURATION","l2Uuid":"l2","origin":"API",
            "operationUuid":"op","ipRangeConfiguration":{"l3Uuid":"l3","ipVersion":4,
            "gatewayAddress":"10.99.44.1/24","ranges":[],"delete":false}}'''
        def range = JSONObjectUtil.toObject(oldIpRange, NetworkConfigChange).ipRangeConfiguration
        assert range.operation == null
        assert range.changedRanges.empty
        assert range.removedRangeUuid == null
        assert !range.delete

        String oldDns = '''{"kind":"DHCP_DNS_CONFIGURATION","l2Uuid":"l2","origin":"API",
            "operationUuid":"op","dhcpDnsConfiguration":{"l3Uuid":"l3","enabled":true,
            "systemTags":[],"ipVersion":4,"dnsServers":["10.99.44.1"]}}'''
        def dns = JSONObjectUtil.toObject(oldDns, NetworkConfigChange).dhcpDnsConfiguration
        assert dns.operation == null
        assert dns.changedDns == null
        assert dns.dnsServers == ["10.99.44.1"]
    }

    @Test
    void testIpv6AddressModeSurvivesQueuedIntentSerialization() {
        def range = new IpRange("ipv6", "2001:db8::10", "2001:db8::20", "SLAAC")
        def change = NetworkConfigChange.replaceIpRangeConfiguration("l2", NetworkOperationOrigin.API,
                "op", "account", "l3", 6, "2001:db8::1/64", [range],
                CollectionChangeOperation.ADD, [range], null)
        def restored = roundTrip(change).ipRangeConfiguration
        assert restored.ranges[0].addressMode == "SLAAC"
        assert restored.changedRanges[0].addressMode == "SLAAC"
        assert new IpRange("old", "10.0.0.10", "10.0.0.20").addressMode == null
    }

    private static NetworkConfigChange roundTrip(NetworkConfigChange change) {
        return JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(change), NetworkConfigChange)
    }
}
