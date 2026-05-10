package org.zstack.test.integration.server

import org.zstack.sdk.PhysicalServerInventory
import org.zstack.sdk.ServerPoolInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class PhysicalServerPowerCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testPowerOnWithOob()
            testPowerOffWithOob()
            testPowerResetWithOob()
            testPowerWithoutOob()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    private ServerPoolInventory createPool(String poolName) {
        def zone = env.inventoryByName("zone") as ZoneInventory
        return createServerPool {
            name = poolName
            zoneUuid = zone.uuid
        } as ServerPoolInventory
    }

    private PhysicalServerInventory createServerWithOob(String serverName, String ip, String poolId) {
        def zone = env.inventoryByName("zone") as ZoneInventory
        return createPhysicalServer {
            name = serverName
            zoneUuid = zone.uuid
            poolUuid = poolId
            managementIp = ip
            oobManagementType = "IPMI"
            oobAddress = "192.168.100.${ip.split('\\.')[3]}"
            oobPort = 623
            oobUsername = "admin"
            oobPassword = "password"
        } as PhysicalServerInventory
    }

    private PhysicalServerInventory createServerWithoutOob(String serverName, String ip, String poolId) {
        def zone = env.inventoryByName("zone") as ZoneInventory
        return createPhysicalServer {
            name = serverName
            zoneUuid = zone.uuid
            poolUuid = poolId
            managementIp = ip
        } as PhysicalServerInventory
    }

    void testPowerOnWithOob() {
        def pool = createPool("pool-power-on")
        def server = createServerWithOob("server-power-on", "192.168.70.1", pool.uuid)

        def result = powerOnPhysicalServer {
            uuid = server.uuid
        } as PhysicalServerInventory

        assert result.uuid == server.uuid
        assert result.powerStatus == "POWER_ON"

        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    void testPowerOffWithOob() {
        def pool = createPool("pool-power-off")
        def server = createServerWithOob("server-power-off", "192.168.70.2", pool.uuid)

        def result = powerOffPhysicalServer {
            uuid = server.uuid
        } as PhysicalServerInventory

        assert result.uuid == server.uuid
        assert result.powerStatus == "POWER_OFF"

        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    void testPowerResetWithOob() {
        def pool = createPool("pool-power-reset")
        def server = createServerWithOob("server-power-reset", "192.168.70.3", pool.uuid)

        def result = powerResetPhysicalServer {
            uuid = server.uuid
        } as PhysicalServerInventory

        assert result.uuid == server.uuid
        assert result.powerStatus == "POWER_ON"

        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }

    void testPowerWithoutOob() {
        def pool = createPool("pool-no-oob")
        def server = createServerWithoutOob("server-no-oob", "192.168.70.4", pool.uuid)

        expect(AssertionError.class) {
            powerOnPhysicalServer {
                uuid = server.uuid
            }
        }

        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }
}
