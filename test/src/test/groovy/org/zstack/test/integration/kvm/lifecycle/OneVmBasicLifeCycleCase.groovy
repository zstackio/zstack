package org.zstack.test.integration.kvm.lifecycle

import org.springframework.http.HttpEntity
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZSClient
import org.zstack.test.integration.kvm.OneVmBasicEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/22.
 */
class OneVmBasicLifeCycleCase extends SubCase {
    EnvSpec env

    def DOC = """
test a VM's start/stop/reboot/destroy/recover operations 
"""

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
        env = OneVmBasicEnv.env()
    }

    @Override
    void test() {
        env.create {
            testStopVm()
        }
    }

    void testStopVm() {
        VmSpec spec = env.specByName("vm")

        KVMAgentCommands.StopVmCmd cmd = null

        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        VmInstanceInventory inv = stopVmInstance {
            uuid = spec.inventory.uuid
        }

        assert inv.state == VmInstanceState.Stopped.toString()

        assert cmd != null
        assert cmd.uuid == spec.inventory.uuid

        def vmvo = dbFindByUuid(cmd.uuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Stopped
    }

    @Override
    void clean() {
        env.delete()
    }
}
