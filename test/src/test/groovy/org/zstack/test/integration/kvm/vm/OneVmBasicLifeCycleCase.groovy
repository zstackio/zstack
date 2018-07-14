package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.cloudbus.CloudBusGlobalConfig
import org.zstack.core.cloudbus.CloudBusGlobalProperty
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
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
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        CloudBusGlobalConfig.STATISTICS_ON.updateValue(true)

        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testStopVm()
            testStartVm()
            testRebootVm()
            testDestroyVm()
            testRecoverVm()
            testDeleteCreatedVm()
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

        String tag = VmSystemTags.VM_SYSTEM_SERIAL_NUMBER.getTag(spec.inventory.uuid)
        assert tag != null


        //TODO: test socketNum, cpuOnSocket
        assert cmd.rootVolume.installPath == vmvo.rootVolume.installPath
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

    void testDeleteCreatedVm() {
        VmSpec spec = env.specByName("vm")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOfferingInventory = env.inventoryByName("instanceOffering")
        ImageInventory imageInventory = env.inventoryByName("image1")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "JustCreatedVm"
        action.rootDiskOfferingUuid = diskOfferingInventory.uuid
        action.instanceOfferingUuid = instanceOfferingInventory.uuid
        action.imageUuid = imageInventory.uuid
        action.l3NetworkUuids = [l3NetworkInventory.uuid]
        action.strategy = VmCreationStrategy.JustCreate.toString()
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result result = action.call()

        destroyVmInstance {
            uuid = result.value.inventory.uuid
        }

        VmInstanceVO vo = dbFindByUuid(result.value.inventory.uuid, VmInstanceVO.class)
        assert vo == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
