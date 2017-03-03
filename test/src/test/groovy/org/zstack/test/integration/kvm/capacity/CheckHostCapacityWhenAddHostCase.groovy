package org.zstack.test.integration.kvm.capacity

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.AddKVMHostAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.Test

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
        env = Env.noHostBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCheckCapacityWhenAddHost()
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
