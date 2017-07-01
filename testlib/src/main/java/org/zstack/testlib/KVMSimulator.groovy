package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import javax.persistence.Tuple

/**
 * Created by xing5 on 2017/6/6.
 */
class KVMSimulator implements Simulator {
    @Override
    void registerSimulators(EnvSpec spec) {
        spec.simulator(KVMConstant.KVM_HOST_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec espec ->
            Spec.checkHttpCallType(e, true)
            def rsp = new KVMAgentCommands.HostCapacityResponse()

            KVMHostSpec kspec = espec.specByUuid(e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID))
            rsp.success = true

            if (kspec == null) {
                rsp.usedCpu = 0
                rsp.cpuNum = 8
                rsp.totalMemory = SizeUnit.GIGABYTE.toByte(32)
                rsp.usedMemory = 0
                rsp.cpuSpeed = 1
                rsp.cpuSockets = 2
            } else {
                rsp.usedCpu = kspec.usedCpu
                rsp.cpuNum = kspec.totalCpu
                rsp.totalMemory = kspec.totalMem
                rsp.usedMemory = kspec.usedMem
                rsp.cpuSpeed = 1
                rsp.cpuSockets = kspec.cpuSockets
            }

            return rsp
        }

        spec.simulator(KVMConstant.KVM_HARDEN_CONSOLE_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_DELETE_CONSOLE_FIREWALL_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_VM_CHECK_STATE) { HttpEntity<String> e ->
            KVMAgentCommands.CheckVmStateCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.CheckVmStateCmd.class)
            List<VmInstanceState> states = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state).in(VmInstanceVO_.uuid, cmd.vmUuids).listValues()
            KVMAgentCommands.CheckVmStateRsp rsp = new KVMAgentCommands.CheckVmStateRsp()
            rsp.states = [:]
            states.each {
                def kstate = KVMConstant.KvmVmState.fromVmInstanceState(it)
                if (kstate != null) {
                    rsp.states[(cmd.hostUuid)] = kstate.toString()
                }
            }

            return rsp
        }

        spec.simulator(KVMConstant.KVM_ATTACH_NIC_PATH) {
            return new KVMAgentCommands.AttachNicResponse()
        }

        spec.simulator(KVMConstant.KVM_DETACH_NIC_PATH) {
            return new KVMAgentCommands.DetachNicRsp()
        }

        spec.simulator(KVMConstant.KVM_ATTACH_ISO_PATH) {
            return new KVMAgentCommands.AttachIsoRsp()
        }

        spec.simulator(KVMConstant.KVM_DETACH_ISO_PATH) {
            return new KVMAgentCommands.DetachIsoRsp()
        }

        spec.simulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) {
            return new KVMAgentCommands.MergeSnapshotRsp()
        }

        spec.simulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) {
            def rsp = new KVMAgentCommands.TakeSnapshotResponse()
            rsp.newVolumeInstallPath = "/new/volume/install/path"
            rsp.snapshotInstallPath = "/snapshot/install/path"
            rsp.size = 1
            return rsp
        }

        spec.simulator(KVMConstant.KVM_PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.PingCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.PingCmd.class)
            assert null != cmd
            assert null != cmd.hostUuid

            def rsp = new KVMAgentCommands.PingResponse()
            rsp.hostUuid = cmd.hostUuid
            return rsp
        }

        spec.simulator(KVMConstant.KVM_CONNECT_PATH) { HttpEntity<String> e ->
            Spec.checkHttpCallType(e, true)
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = true
            rsp.libvirtVersion = "1.0.0"
            rsp.qemuVersion = "1.3.0"
            rsp.iptablesSucc = true
            return rsp
        }

        spec.simulator(KVMConstant.KVM_ECHO_PATH) { HttpEntity<String> e ->
            Spec.checkHttpCallType(e, true)
            return [:]
        }

        spec.simulator(KVMConstant.KVM_DETACH_VOLUME) {
            return new KVMAgentCommands.DetachDataVolumeResponse()
        }

        spec.simulator(KVMConstant.KVM_VM_SYNC_PATH) { HttpEntity<String> e ->
            def hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            List<Tuple> states = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.uuid, VmInstanceVO_.state)
                    .in(VmInstanceVO_.state, [VmInstanceState.Running, VmInstanceState.Unknown])
                    .eq(VmInstanceVO_.hostUuid, hostUuid).listTuple()

            def rsp = new KVMAgentCommands.VmSyncResponse()
            rsp.states = [:]
            states.each {
                String vmUuid = it.get(0, String.class)
                VmInstanceState state = it.get(1, VmInstanceState.class)
                if (state == VmInstanceState.Unknown) {
                    // host reconnecting will set VMs to Unknown in DB
                    // the spec.simulator treat them as Running by default
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.Running.toString()
                } else {
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.fromVmInstanceState(state).toString()
                }
            }

            return rsp
        }

        spec.simulator(KVMConstant.KVM_ATTACH_VOLUME) {
            return new KVMAgentCommands.AttachDataVolumeResponse()
        }

        spec.simulator(KVMConstant.KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH) { HttpEntity<String> e ->
            Spec.checkHttpCallType(e, true)
            return new KVMAgentCommands.CheckPhysicalNetworkInterfaceResponse()
        }

        spec.simulator(KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CreateBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) {
            return new KVMAgentCommands.MigrateVmResponse()
        }

        spec.simulator(KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CheckBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_CHECK_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CheckVlanBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CreateVlanBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_START_VM_PATH) {
            return new KVMAgentCommands.StartVmResponse()
        }

        spec.simulator(KVMConstant.KVM_STOP_VM_PATH) {
            return new KVMAgentCommands.StopVmResponse()
        }

        spec.simulator(KVMConstant.KVM_PAUSE_VM_PATH) {
            return new KVMAgentCommands.PauseVmResponse()
        }

        spec.simulator(KVMConstant.KVM_RESUME_VM_PATH) {
            return new KVMAgentCommands.ResumeVmResponse()
        }

        spec.simulator(KVMConstant.KVM_REBOOT_VM_PATH) {
            return new KVMAgentCommands.RebootVmResponse()
        }

        spec.simulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            return new KVMAgentCommands.DestroyVmResponse()
        }

        spec.simulator(KVMConstant.KVM_GET_VNC_PORT_PATH) {
            def rsp = new KVMAgentCommands.GetVncPortResponse()
            rsp.port = 5900
            return rsp
        }

        spec.simulator(KVMConstant.KVM_LOGOUT_ISCSI_PATH) {
            return new KVMAgentCommands.LogoutIscsiTargetRsp()
        }

        spec.simulator(KVMConstant.KVM_LOGIN_ISCSI_PATH) {
            return new KVMAgentCommands.LoginIscsiTargetRsp()
        }

        spec.simulator(KVMConstant.KVM_VM_ONLINE_INCREASE_CPU) {
            return new KVMAgentCommands.IncreaseCpuResponse()
        }

        spec.simulator(KVMConstant.KVM_VM_ONLINE_INCREASE_MEMORY) {
            return new KVMAgentCommands.IncreaseMemoryResponse()
        }
    }
}
