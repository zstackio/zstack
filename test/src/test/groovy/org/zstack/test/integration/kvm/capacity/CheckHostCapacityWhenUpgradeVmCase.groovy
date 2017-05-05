package org.zstack.test.integration.kvm.capacity

import org.zstack.header.vm.VmInstanceState
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/3/13.
 */
class CheckHostCapacityWhenUpgradeVmCase extends SubCase {
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
            testCheckHostCapacityWhenUpgradeVm()
        }
    }

    void testCheckHostCapacityWhenUpgradeVm() {

        HostInventory host = env.inventoryByName("kvm")
        VmInstanceInventory vm = env.inventoryByName("vm")
        long originAvailableCpuCapacity = host.availableCpuCapacity
        long originAvailableMemoryCapacity = host.availableMemoryCapacity


        // create vm, and start vm
        VmInstanceInventory newVm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            hostUuid = host.uuid
            sessionId = currentEnvSpec.session.uuid
        }
        if(VmInstanceState.Stopped.name() == newVm.getState()){
            startVmInstance {
                uuid = newVm.uuid
            }
        }


        // check host capacity : origin == current + newVm
        host = queryHost {
            conditions=["uuid=${host.uuid}".toString()]
        }[0]
        assert originAvailableCpuCapacity == newVm.cpuNum + host.availableCpuCapacity
        assert originAvailableMemoryCapacity == newVm.memorySize + host.availableMemoryCapacity


        // upgrade vm, check host capacity : origin == current + newVm
        newVm = updateVmInstance {
            uuid = newVm.uuid
            cpuNum = newVm.cpuNum * 2
            memorySize = newVm.memorySize * 2
        }
        host = queryHost {
            conditions=["uuid=${host.uuid}".toString()]
        }[0]
        assert originAvailableCpuCapacity == newVm.cpuNum + host.availableCpuCapacity
        assert originAvailableMemoryCapacity == newVm.memorySize + host.availableMemoryCapacity

    }

    @Override
    void clean() {
        env.delete()
    }
}
