package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands.*
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil


class KvmHypervisorInfoLifecycleCase extends SubCase {
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
            VmInstanceInventory vm = env.inventoryByName("vm")
            retryInSecs {
                assert Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.uuid).count() == 1
            }
            assert Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.hostUuid).count() == 1

            String reportedVersion = "4.2.0-628.g36ee592.el7"
            env.afterSimulator(KVMConstant.GET_VIRTUALIZER_INFO_PATH) { GetVirtualizerInfoRsp rsp ->
                rsp.hostInfo.version = reportedVersion
                rsp.vmInfoList.each { it.version = reportedVersion }
                return rsp
            }
            env.simulator(KVMConstant.KVM_REBOOT_VM_PATH) { HttpEntity<String> e ->
                RebootVmCmd cmd = JSONObjectUtil.toObject(e.body, RebootVmCmd.class)
                return new RebootVmResponse(virtualizerInfo: new VirtualizerInfoTO(
                        uuid: cmd.vmUuid, virtualizer: "qemu-kvm", version: reportedVersion))
            }
            env.afterSimulator(KVMConstant.KVM_SYNC_VM_DEVICEINFO_PATH) { VmDevicesInfoResponse rsp ->
                rsp.virtualizerInfo.version = reportedVersion
                return rsp
            }
            env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { StartVmResponse rsp ->
                rsp.virtualizerInfo.version = reportedVersion
                return rsp
            }
            try {
                reconnectHost { uuid = vm.hostUuid }
                rebootVmInstance { uuid = vm.uuid }
                [vm.uuid, vm.hostUuid].each { String resourceUuid ->
                    assert Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, resourceUuid).count() == 1
                    KvmHypervisorInfoVO info = dbFindByUuid(resourceUuid, KvmHypervisorInfoVO.class)
                    assert info.version == reportedVersion
                    assert info.hypervisor == "qemu-kvm"
                }

                stopVmInstance { uuid = vm.uuid }
                assert !Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.uuid).isExists()
                startVmInstance { uuid = vm.uuid }
                retryInSecs {
                    assert Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.uuid).count() == 1
                }
            } finally {
                env.httpPostHandlers.remove(KVMConstant.GET_VIRTUALIZER_INFO_PATH)
                env.httpPostHandlers.remove(KVMConstant.KVM_SYNC_VM_DEVICEINFO_PATH)
                env.httpPostHandlers.remove(KVMConstant.KVM_START_VM_PATH)
                env.simulator(KVMConstant.KVM_REBOOT_VM_PATH) { new RebootVmResponse() }
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
