package org.zstack.test.integration.kvm.capacity

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.KVMHostSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 2017/3/3.
 */
class CannotUseHostReservedResource extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testUseHostReservedMemory()
        }
    }

    void testUseHostReservedMemory() {
        ClusterSpec clusterSpec = env.specByName("cluster")
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("1G")
        KVMHostSpec kvmHostSpec = env.specByName("kvm")
        kvmHostSpec.setTotalMem(SizeUnit.GIGABYTE.toByte(2))
        InstanceOfferingSpec iospec = env.getSpecsByName("instanceOffering")
        iospec.memory = SizeUnit.GIGABYTE.toByte(2)

        env.afterSimulator(KVMConstant.KVM_HOST_CAPACITY_PATH) { rsp, HttpEntity<String> e ->
            rsp as KVMAgentCommands.HostCapacityResponse
            rsp.setTotalMemory(1)
            return rsp
        }
        def action = new CreateVmInstanceAction()
        action.name = "vm2"
        action.instanceOfferingUuid = iospec.inventory.uuid
        action.hostUuid = kvmHostSpec.inventory.uuid

        CreateVmInstanceAction.Result res = action.call()
        assert res.error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
