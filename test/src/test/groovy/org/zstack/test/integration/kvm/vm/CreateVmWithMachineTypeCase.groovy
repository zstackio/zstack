package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * @author: kefeng.wang
 * @date: 2018-09-28 16:18
 */
class CreateVmWithMachineTypeCase extends SubCase {
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
            createVmWithMachineType("pc")
            createVmWithMachineType("q35")
        }
    }

    void createVmWithMachineType(String machineTypeValue) {
        VmInstanceInventory vm = env.inventoryByName("vm")
        assert null != vm

        CreateVmInstanceAction action = new CreateVmInstanceAction(
                name: "vm-" + machineTypeValue,
                instanceOfferingUuid: vm.instanceOfferingUuid,
                l3NetworkUuids: [vm.defaultL3NetworkUuid],
                imageUuid: vm.imageUuid,
                sessionId: currentEnvSpec.session.uuid
        )
        if (machineTypeValue == null) {
            machineTypeValue = "pc"
        } else {
            action.machineType = machineTypeValue
        }
        CreateVmInstanceAction.Result result = action.call()
        String vmUuid = result.value.inventory.getUuid()

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${vmUuid}",
                    "tag=${VmSystemTags.VM_MACHINE_TYPE_TOKEN}::${machineTypeValue}"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        VmInstanceVO vo = dbFindByUuid(vmUuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
    }

    @Override
    void clean() {
        env.delete()
    }
}
