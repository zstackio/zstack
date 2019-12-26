package org.zstack.test.integration.kvm.capacity

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.GetCpuMemoryCapacityResult
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.kvm.host.HostEnv
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
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
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCheckCapacityWhenAddHost()
            testCreateVmWithReserveCapacity()
        }
    }

    void testCreateVmWithReserveCapacity() {
        def image = env.inventoryByName("image1")
        def l3 = env.inventoryByName("l3")
        def instanceOffering = env.inventoryByName("instanceOffering")
        def cluster = env.inventoryByName("cluster")

        expect(AssertionError.class) {
            createVmInstance {
                name = "vm"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = instanceOffering.uuid
                clusterUuid = cluster.uuid
            }
        }

        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.name
            value = "1G"
            resourceUuid = cluster.uuid
        }

        GetCpuMemoryCapacityResult result = getCpuMemoryCapacity {
            clusterUuids = [cluster.uuid]
        }

        KVMGlobalConfig.RESERVED_CPU_CAPACITY.updateValue(result.availableCpu)

        createVmInstance {
            name = "vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            clusterUuid = cluster.uuid
        }
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
        action.managementIp = "localhost"
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
