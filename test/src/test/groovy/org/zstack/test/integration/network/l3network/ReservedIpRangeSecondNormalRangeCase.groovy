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
            reserveIpAddressFromSecondNormalRange()
        }
    }

    void reserveIpAddressFromSecondNormalRange() {
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

        ReservedIpRangeInventory reservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3.uuid
            startIp = "192.168.250.36"
            endIp = "192.168.250.36"
        }

        assertReservedUsedIp(l3.uuid, "192.168.250.36", secondIpRange.uuid, reservedIpRange.uuid)
        assertIpUnavailable(l3.uuid, "192.168.250.36")
    }

    void assertReservedUsedIp(String l3NetworkUuid, String ip, String ipRangeUuid, String reservedIpRangeUuid) {
        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3NetworkUuid)
                .eq(UsedIpVO_.metaData, reservedIpRangeUuid)
                .find()
        assert usedIp != null
        assert usedIp.ip == ip
        assert usedIp.ipRangeUuid == ipRangeUuid
        assert usedIp.usedFor == IpAllocatedReason.Reserved.toString()
        assert usedIp.metaData == reservedIpRangeUuid
    }

    void assertIpUnavailable(String l3NetworkUuid, String ip) {
        CheckIpAvailabilityAction check = new CheckIpAvailabilityAction()
        check.ip = ip
        check.l3NetworkUuid = l3NetworkUuid
        check.sessionId = adminSession()
        CheckIpAvailabilityAction.Result result = check.call()
        assert result.error == null
        assert result.value.available == false
        assert result.value.reason == IpNotAvailabilityReason.USED.toString()
    }
}
