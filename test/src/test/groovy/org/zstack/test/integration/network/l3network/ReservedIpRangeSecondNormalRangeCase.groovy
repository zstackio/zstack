package org.zstack.test.integration.network.l3network

import org.zstack.core.db.Q
import org.zstack.header.network.IpAllocatedReason
import org.zstack.header.network.l3.L3NetworkCategory
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.network.l3.IpNotAvailabilityReason
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

class ReservedIpRangeSecondNormalRangeCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.OneIpL3Network()
    }

    @Override
    void test() {
        env.create {
            reserveIpv4AddressFromSecondNormalRange()
            reserveIpv6AddressFromSecondNormalRange()
        }
    }

    void reserveIpv4AddressFromSecondNormalRange() {
        L3NetworkInventory sourceL3Network = env.inventoryByName("l3-1")
        L3NetworkInventory l3 = createL3Network {
            name = "l3-reserve-ip-in-second-range"
            l2NetworkUuid = sourceL3Network.l2NetworkUuid
            category = L3NetworkCategory.Private
        }

        addIpRange {
            name = "first-ip-range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.250.2"
            endIp = "192.168.250.30"
            gateway = "192.168.250.1"
            netmask = "255.255.255.0"
        }

        IpRangeInventory secondIpRange = addIpRange {
            name = "second-ip-range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.250.31"
            endIp = "192.168.250.50"
            gateway = "192.168.250.1"
            netmask = "255.255.255.0"
        }

        IpRangeInventory singleIpRange = addIpRange {
            name = "single-ip-range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.250.51"
            endIp = "192.168.250.51"
            gateway = "192.168.250.1"
            netmask = "255.255.255.0"
        }

        ReservedIpRangeInventory reservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3.uuid
            startIp = "192.168.250.40"
            endIp = "192.168.250.41"
        }

        ReservedIpRangeInventory singleReservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3.uuid
            startIp = "192.168.250.51"
            endIp = "192.168.250.51"
        }

        assertReservedUsedIp(l3.uuid, "192.168.250.40", secondIpRange.uuid, reservedIpRange.uuid)
        assertReservedUsedIp(l3.uuid, "192.168.250.41", secondIpRange.uuid, reservedIpRange.uuid)
        assertReservedUsedIp(l3.uuid, "192.168.250.51", singleIpRange.uuid, singleReservedIpRange.uuid)
        assertIpUnavailable(l3.uuid, "192.168.250.40")
        assertIpUnavailable(l3.uuid, "192.168.250.41")
        assertIpUnavailable(l3.uuid, "192.168.250.51")
    }

    void reserveIpv6AddressFromSecondNormalRange() {
        L3NetworkInventory sourceL3Network = env.inventoryByName("l3-1")
        L3NetworkInventory l3 = createL3Network {
            name = "l3-reserve-ipv6-in-second-range"
            l2NetworkUuid = sourceL3Network.l2NetworkUuid
            category = L3NetworkCategory.Private
        }

        addIpv6Range {
            name = "first-ipv6-range"
            l3NetworkUuid = l3.uuid
            startIp = "2024:5:28::2"
            endIp = "2024:5:28::30"
            gateway = "2024:5:28::1"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        IpRangeInventory secondIpRange = addIpv6Range {
            name = "second-ipv6-range"
            l3NetworkUuid = l3.uuid
            startIp = "2024:5:28::31"
            endIp = "2024:5:28::50"
            gateway = "2024:5:28::1"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        IpRangeInventory singleIpRange = addIpv6Range {
            name = "single-ipv6-range"
            l3NetworkUuid = l3.uuid
            startIp = "2024:5:28::51"
            endIp = "2024:5:28::51"
            gateway = "2024:5:28::1"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        ReservedIpRangeInventory reservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3.uuid
            startIp = "2024:5:28::40"
            endIp = "2024:5:28::41"
        }

        ReservedIpRangeInventory singleReservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3.uuid
            startIp = "2024:5:28::51"
            endIp = "2024:5:28::51"
        }

        assertReservedUsedIp(l3.uuid, "2024:5:28::40", secondIpRange.uuid, reservedIpRange.uuid)
        assertReservedUsedIp(l3.uuid, "2024:5:28::41", secondIpRange.uuid, reservedIpRange.uuid)
        assertReservedUsedIp(l3.uuid, "2024:5:28::51", singleIpRange.uuid, singleReservedIpRange.uuid)
        assertIpUnavailable(l3.uuid, "2024:5:28::40")
        assertIpUnavailable(l3.uuid, "2024:0005:0028:0000:0000:0000:0000:0040")
        assertIpUnavailable(l3.uuid, "2024:0005:0028:0000:0000:0000:0000:0001", IpNotAvailabilityReason.GATEWAY.toString())
        assertIpUnavailable(l3.uuid, "2024:5:28::41")
        assertIpUnavailable(l3.uuid, "2024:5:28::51")
    }

    void assertReservedUsedIp(String l3NetworkUuid, String ip, String ipRangeUuid, String reservedIpRangeUuid) {
        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3NetworkUuid)
                .eq(UsedIpVO_.ip, ip)
                .eq(UsedIpVO_.metaData, reservedIpRangeUuid)
                .find()
        assert usedIp != null
        assert usedIp.ip == ip
        assert usedIp.ipRangeUuid == ipRangeUuid
        assert usedIp.usedFor == IpAllocatedReason.Reserved.toString()
        assert usedIp.metaData == reservedIpRangeUuid
    }

    void assertIpUnavailable(String l3NetworkUuid, String ip) {
        assertIpUnavailable(l3NetworkUuid, ip, IpNotAvailabilityReason.USED.toString())
    }

    void assertIpUnavailable(String l3NetworkUuid, String ip, String reason) {
        CheckIpAvailabilityAction check = new CheckIpAvailabilityAction()
        check.ip = ip
        check.l3NetworkUuid = l3NetworkUuid
        check.sessionId = adminSession()
        CheckIpAvailabilityAction.Result result = check.call()
        assert result.error == null
        assert result.value.available == false
        assert result.value.reason == reason
    }
}
