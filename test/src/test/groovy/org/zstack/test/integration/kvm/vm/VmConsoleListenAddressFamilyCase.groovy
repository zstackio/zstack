package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.ManagementNetworkIpVersionUtils

class VmConsoleListenAddressFamilyCase extends SubCase {
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
            testZSTAC87496StartVmCommandUsesZoneManagementNetworkIpVersion()
        }
    }

    void testZSTAC87496StartVmCommandUsesZoneManagementNetworkIpVersion() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        ZoneInventory zone = env.inventoryByName("zone") as ZoneInventory
        SystemTagVO tag = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, zone.uuid)
                .like(SystemTagVO_.tag, "managementNetwork::ipVersion::%")
                .find()

        stopVmInstance {
            uuid = vm.uuid
        }

        updateSystemTag {
            uuid = tag.uuid
            tag = "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV6}"
        }

        KVMAgentCommands.StartVmCmd ipv6Command = startVmAndCaptureCommand(vm.uuid)
        assert ipv6Command.managementNetworkIpVersion == ManagementNetworkIpVersionUtils.IPV6

        stopVmInstance {
            uuid = vm.uuid
        }

        updateSystemTag {
            uuid = tag.uuid
            tag = "managementNetwork::ipVersion::${ManagementNetworkIpVersionUtils.IPV4}"
        }

        KVMAgentCommands.StartVmCmd ipv4Command = startVmAndCaptureCommand(vm.uuid)
        assert ipv4Command.managementNetworkIpVersion == ManagementNetworkIpVersionUtils.IPV4
    }

    private KVMAgentCommands.StartVmCmd startVmAndCaptureCommand(String vmUuid) {
        KVMAgentCommands.StartVmCmd command = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> entity ->
            command = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        startVmInstance {
            uuid = vmUuid
        }

        assert command != null
        return command
    }

    @Override
    void clean() {
        env.delete()
    }
}
