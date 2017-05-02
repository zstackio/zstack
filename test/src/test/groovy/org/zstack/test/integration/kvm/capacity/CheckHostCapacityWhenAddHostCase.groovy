package org.zstack.test.integration.kvm.capacity

import org.springframework.http.HttpEntity
import org.zstack.header.vm.VmInstanceState
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetCpuMemoryCapacityResult
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.test.integration.kvm.Env

/**
 * Created by zouye on 2017/3/1.
 */
class CheckHostCapacityWhenAddHostCase extends SubCase {
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
            testCheckCapacityWhenAddHost()
            testCPUCapacityCalculation()
        }
    }

    void testCPUCapacityCalculation() {
        ClusterInventory clusterInventory = env.inventoryByName("cluster")
        VmInstanceInventory vmInstanceInventory = env.inventoryByName("vm")

        GetCpuMemoryCapacityResult ret1 = getCpuMemoryCapacity {
            clusterUuids = [clusterInventory.uuid]
        }
        updateVmInstance {
            uuid = vmInstanceInventory.uuid
            cpuNum = 10
        }
        GetCpuMemoryCapacityResult ret2 = getCpuMemoryCapacity {
            clusterUuids = [clusterInventory.uuid]
        }
        assert ret1.availableCpu == ret2.availableCpu

        VmInstanceInventory rebootVmInstanceResult = rebootVmInstance {
            uuid = vmInstanceInventory.uuid
        }

        assert rebootVmInstanceResult.state == VmInstanceState.Running.toString()

        GetCpuMemoryCapacityResult ret3 = getCpuMemoryCapacity {
            clusterUuids = [clusterInventory.uuid]
        }
        assert ret3.availableCpu == ret2.availableCpu - 10

        stopVmInstance {
            uuid = vmInstanceInventory.uuid
        }
        GetCpuMemoryCapacityResult ret4 = getCpuMemoryCapacity {
            clusterUuids = [clusterInventory.uuid]
        }
        assert ret4.availableCpu == ret2.availableCpu
    }

    void testCheckCapacityWhenAddHost() {
        ClusterSpec clusterSpec = env.specByName("cluster")
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("1024G")

        env.afterSimulator(KVMConstant.KVM_HOST_CAPACITY_PATH) { rsp, HttpEntity<String> e ->
            rsp as KVMAgentCommands.HostCapacityResponse
            rsp.setTotalMemory(1)
            return rsp
        }

        def action = new AddKVMHostAction()
        action.username = "root"
        action.password = "password"
        action.name = "addHost"
        action.managementIp = "127.0.0.2"
        action.clusterUuid = clusterSpec.inventory.uuid
        action.sessionId = adminSession()

        AddKVMHostAction.Result res = action.call()
        assert res.error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
