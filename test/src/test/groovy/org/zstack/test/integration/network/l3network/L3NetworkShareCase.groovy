package org.zstack.test.integration.network.l3network

import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase


import static java.util.Arrays.asList

/**
 * Created by Boce on 2021-09-03.
 */
class L3NetworkShareCase extends SubCase{
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
            testL3NetworkShareResourceAndIpRange()
        }
    }

    void testL3NetworkShareResourceAndIpRange() {
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")

        addIpRange {
            name = "ipr-3"
            l3NetworkUuid = l3_1.getUuid()
            startIp = "192.168.100.101"
            endIp = "192.168.100.200"
            gateway = "192.168.100.1"
            netmask = "255.255.255.0"
        }

        AccountInventory account = createAccount {
            name = "test"
            password = "password"
        }
        SessionInventory sessionInventory = logInByAccount {
            accountName = "test"
            password = "password"
        }

        def range = queryIpRange {
            conditions=["l3NetworkUuid=${l3_1.uuid}".toString()]
            sessionId = sessionInventory.uuid
        }[0] as IpRangeInventory
        assert range == null

        shareResource {
            resourceUuids = [l3_1.getUuid()]
            accountUuids = [account.uuid]
        }

        range = queryIpRange {
            conditions=["l3NetworkUuid=${l3_1.uuid}".toString()]
            sessionId = sessionInventory.uuid
        }[0] as IpRangeInventory
        assert range.name == "ipr-3"

        addIpRange {
            name = "ipr-4"
            l3NetworkUuid = l3_1.getUuid()
            startIp = "192.168.100.90"
            endIp = "192.168.100.100"
            gateway = "192.168.100.1"
            netmask = "255.255.255.0"
        }

        range = queryIpRange {
            conditions=["l3NetworkUuid=${l3_1.uuid}".toString(), "name=ipr-4"]
            sessionId = sessionInventory.uuid
        }[0] as IpRangeInventory
        assert range.name == "ipr-4"
    }
}
