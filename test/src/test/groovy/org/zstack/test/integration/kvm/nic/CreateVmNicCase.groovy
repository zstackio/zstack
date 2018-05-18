package org.zstack.test.integration.kvm.nic

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2018/5/18.
 */
class CreateVmNicCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmNic()
        }
    }

    void testCreateVmNic() {
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmNicInventory nic = createVmNic {
            l3NetworkUuid = l3.uuid
        }
        assert nic.uuid != null
        assert nic.l3NetworkUuid == l3.uuid
        assert nic.deviceId == 0
        assert nic.vmInstanceUuid == null
        assert nic.ip != null
        assert nic.mac != null

        IpRangeInventory ipRangeInventory = l3.ipRanges.get(0)
        assert nic.gateway == ipRangeInventory.gateway
        assert nic.netmask == ipRangeInventory.netmask
    }

    @Override
    void clean() {
        env.delete()
    }
}
