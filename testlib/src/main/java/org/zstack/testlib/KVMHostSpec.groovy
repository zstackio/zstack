package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.HostInventory
import org.zstack.utils.gson.JSONObjectUtil

import javax.persistence.Tuple

/**
 * Created by xing5 on 2017/2/12.
 */
class KVMHostSpec extends HostSpec {
    @SpecParam
    String username = "root"
    @SpecParam
    String password = "password"

    KVMHostSpec(EnvSpec envSpec) {
        super(envSpec)

        preCreate {
            setupSimulator()
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addKVMHost {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.managementIp = managementIp
            delegate.username = username
            delegate.password = password
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.clusterUuid = (parent as ClusterSpec).inventory.uuid
            delegate.sessionId = sessionId
        } as HostInventory

        postCreate {
            inventory = queryHost {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    private setupSimulator() {
        simulator(KVMConstant.KVM_HOST_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def rsp = new KVMAgentCommands.HostCapacityResponse()
            KVMHostSpec spec = espec.specByUuid(e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID))
            rsp.success = true
            rsp.usedCpu = spec.usedCpu
            rsp.cpuNum = spec.totalCpu
            rsp.totalMemory = spec.totalMem
            rsp.usedMemory = spec.usedMem
            rsp.cpuSpeed = 1
            return rsp
        }

        simulator(KVMConstant.KVM_HARDEN_CONSOLE_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        simulator(KVMConstant.KVM_DELETE_CONSOLE_FIREWALL_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        simulator(KVMConstant.KVM_VM_CHECK_STATE) { HttpEntity<String> e ->
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

        simulator(KVMConstant.KVM_ATTACH_NIC_PATH) {
            return new KVMAgentCommands.AttachNicResponse()
        }

        simulator(KVMConstant.KVM_DETACH_NIC_PATH) {
            return new KVMAgentCommands.DetachNicRsp()
        }

        simulator(KVMConstant.KVM_ATTACH_ISO_PATH) {
            return new KVMAgentCommands.AttachIsoRsp()
        }

        simulator(KVMConstant.KVM_DETACH_ISO_PATH) {
            return new KVMAgentCommands.DetachIsoRsp()
        }

        simulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) {
            return new KVMAgentCommands.MergeSnapshotRsp()
        }

        simulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) {
            def rsp = new KVMAgentCommands.TakeSnapshotResponse()
            rsp.newVolumeInstallPath = "/new/volume/install/path"
            rsp.snapshotInstallPath = "/snapshot/install/path"
            rsp.size = 1
            return rsp
        }

        simulator(KVMConstant.KVM_PING_PATH) {
            def rsp = new KVMAgentCommands.PingResponse()
            rsp.hostUuid = inventory.uuid
            return rsp
        }

        simulator(KVMConstant.KVM_CONNECT_PATH) {
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = true
            rsp.libvirtVersion = "1.0.0"
            rsp.qemuVersion = "1.3.0"
            rsp.iptablesSucc = true
            return rsp
        }

        simulator(KVMConstant.KVM_ECHO_PATH) {
            return [:]
        }

        simulator(KVMConstant.KVM_DETACH_VOLUME) {
            return new KVMAgentCommands.DetachDataVolumeResponse()
        }

        simulator(KVMConstant.KVM_VM_SYNC_PATH) { HttpEntity<String> e ->
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
                    // the simulator treat them as Running by default
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.Running.toString()
                } else {
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.fromVmInstanceState(state).toString()
                }
            }

            return rsp
        }

        simulator(KVMConstant.KVM_ATTACH_VOLUME) {
            return new KVMAgentCommands.AttachDataVolumeResponse()
        }

        simulator(KVMConstant.KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH) {
            return new KVMAgentCommands.CheckPhysicalNetworkInterfaceResponse()
        }

        simulator(KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CreateBridgeResponse()
        }

        simulator(KVMConstant.KVM_MIGRATE_VM_PATH) {
            return new KVMAgentCommands.MigrateVmResponse()
        }

        simulator(KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CheckBridgeResponse()
        }

        simulator(KVMConstant.KVM_CHECK_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CheckVlanBridgeResponse()
        }

        simulator(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CreateVlanBridgeResponse()
        }

        simulator(KVMConstant.KVM_START_VM_PATH) {
            return new KVMAgentCommands.StartVmResponse()
        }

        simulator(KVMConstant.KVM_STOP_VM_PATH) {
            return new KVMAgentCommands.StopVmResponse()
        }

        simulator(KVMConstant.KVM_PAUSE_VM_PATH) {
            return new KVMAgentCommands.PauseVmResponse()
        }

        simulator(KVMConstant.KVM_RESUME_VM_PATH) {
            return new KVMAgentCommands.ResumeVmResponse()
        }

        simulator(KVMConstant.KVM_REBOOT_VM_PATH) {
            return new KVMAgentCommands.RebootVmResponse()
        }

        simulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            return new KVMAgentCommands.DestroyVmResponse()
        }

        simulator(KVMConstant.KVM_GET_VNC_PORT_PATH) {
            def rsp = new KVMAgentCommands.GetVncPortResponse()
            rsp.port = 5900
            return rsp
        }

        simulator(KVMConstant.KVM_LOGOUT_ISCSI_PATH) {
            return new KVMAgentCommands.LogoutIscsiTargetRsp()
        }

        simulator(KVMConstant.KVM_LOGIN_ISCSI_PATH) {
            return new KVMAgentCommands.LoginIscsiTargetRsp()
        }
    }
}
