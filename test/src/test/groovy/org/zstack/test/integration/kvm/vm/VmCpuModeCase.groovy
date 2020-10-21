package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.*;
import org.zstack.core.cloudbus.CloudBusGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.header.vm.VmInstanceEO
import org.zstack.header.vm.VmInstanceEO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeEO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.l3.NetworkGlobalProperty
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.kvm.KVMGlobalConfig

class VmCpuModeCase extends SubCase {
    EnvSpec env

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

            stopVM()
            testCpuModeWhenVmStart("none")

            setVmCpuModeFromResouceConfig("host-model");

            stopVM()
            testCpuModeWhenVmStart("host-model")

        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void setVmCpuModeFromResouceConfig(String CpuMode) {

        VmSpec spec = env.specByName("vm")

        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.NESTED_VIRTUALIZATION.name
            value = CpuMode
            resourceUuid = spec.inventory.uuid
        }

    }

    void testCpuModeWhenVmStart(String expectCpuMode) {

        VmSpec spec = env.specByName("vm")

        KVMAgentCommands.StartVmCmd cmdStart = null

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmdStart = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        VmInstanceInventory invStart = startVmInstance {
            uuid = spec.inventory.uuid
        }

        assert cmdStart != null
        assert cmdStart.nestedVirtualization == expectCpuMode;

    }

    void stopVM(){

        VmSpec spec = env.specByName("vm")

        KVMAgentCommands.StopVmCmd cmdStop = null

        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmdStop = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        VmInstanceInventory invStop = stopVmInstance {
            uuid = spec.inventory.uuid
        }

        assert cmdStop != null

    }


}
