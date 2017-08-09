package org.zstack.test.integration.networkservice.provider

import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by MaJin on 2017-08-09.
 */
class APITrimCase extends SubCase{
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        env.create {
            L3NetworkInventory pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
            createVip {
                name = "testVip"
                l3NetworkUuid = pubL3.uuid
                requiredIp = " 12.16.10.11"
            }
        }
    }
}
