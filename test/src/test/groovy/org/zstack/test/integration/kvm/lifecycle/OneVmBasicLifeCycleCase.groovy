package org.zstack.test.integration.kvm.lifecycle

import org.springframework.http.HttpEntity
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
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
            testStartVm()
            testRebootVm()
            testDestroyVm()
            testRecoverVm()
        }
    }

    void testRecoverVm() {
        VmSpec spec = env.specByName("vm")

        VmInstanceInventory inv = recoverVmInstance {
            uuid = spec.inventory.uuid
        }

        assert inv.state == VmInstanceState.Stopped.toString()

        // confirm the vm can start after being recovered
        testStartVm()
    }

    void testDestroyVm() {
        VmSpec spec = env.specByName("vm")

        KVMAgentCommands.DestroyVmCmd cmd = null

        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        destroyVmInstance {
            uuid = spec.inventory.uuid
        }

        assert cmd != null
        assert cmd.uuid == spec.inventory.uuid
        VmInstanceVO vmvo = dbFindByUuid(cmd.uuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Destroyed
    }

    void testRebootVm() {
        // reboot = stop + start
        VmSpec spec = env.specByName("vm")

        KVMAgentCommands.StartVmCmd startCmd = null
        KVMAgentCommands.StopVmCmd stopCmd = null

        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            stopCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            startCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        VmInstanceInventory inv = rebootVmInstance {
            uuid = spec.inventory.uuid
        }

        assert startCmd != null
        assert startCmd.vmInstanceUuid == spec.inventory.uuid
        assert stopCmd != null
        assert stopCmd.uuid == spec.inventory.uuid
        assert inv.state == VmInstanceState.Running.toString()
    }

    void testStartVm() {
        VmSpec spec = env.specByName("vm")

        KVMAgentCommands.StartVmCmd cmd = null

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        VmInstanceInventory inv = startVmInstance {
            uuid = spec.inventory.uuid
        }

        assert cmd != null
        assert cmd.vmInstanceUuid == spec.inventory.uuid
        assert inv.state == VmInstanceState.Running.toString()

        VmInstanceVO vmvo = dbFindByUuid(cmd.vmInstanceUuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Running
        assert cmd.vmInternalId == vmvo.internalId
        assert cmd.vmName == vmvo.name
        assert cmd.memory == vmvo.memorySize
        assert cmd.cpuNum == vmvo.cpuNum
        //TODO: test socketNum, cpuOnSocket
        assert cmd.rootVolume.installPath == vmvo.rootVolumes.installPath
        assert cmd.useVirtio
        vmvo.vmNics.each { nic ->
            KVMAgentCommands.NicTO to = cmd.nics.find { nic.mac == it.mac }
            assert to != null: "unable to find the nic[mac:${nic.mac}]"
            assert to.deviceId == nic.deviceId
            assert to.useVirtio
            assert to.nicInternalName == nic.internalName
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
