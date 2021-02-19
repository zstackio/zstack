package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.vm.VmInstanceState
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by yaoning.li on 2021/2/19.
 */
class VmCascadeProtectionCase extends SubCase {
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
            ZoneInventory zoneInventory = env.inventoryByName("zone")
            VmInstanceInventory vm = env.inventoryByName("vm")

            VmGlobalConfig.CASCADE_ALLOWS_VM_STATUS.updateValue(VmInstanceState.Stopped.name())
            expectError {
                deleteZone {
                    uuid = zoneInventory.uuid
                }
            }

            detachPrimaryStorageFromCluster {
                clusterUuid = env.inventoryByName("cluster").uuid
                primaryStorageUuid = env.inventoryByName("local").uuid
            }

            stopVmInstance {
                uuid = vm.uuid
            }
            deleteZone {
                uuid = zoneInventory.uuid
            }
        }
    }


    @Override
    void clean() {
        env.delete()
    }
}
