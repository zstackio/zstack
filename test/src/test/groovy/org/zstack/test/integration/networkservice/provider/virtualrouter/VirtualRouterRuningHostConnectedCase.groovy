package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by heathhose on 17-3-25.
 */
class VirtualRouterRuningHostConnectedCase extends SubCase{
    def DOC = """
use:
test the vr is set to never stop
"""
    EnvSpec env
    DatabaseFacade dbf
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            reconnectedHostAndCheckVR()
        }
    }

    void reconnectedHostAndCheckVR(){
        HostInventory host1 = env.inventoryByName("kvm")
        VmInstanceInventory vmi = env.inventoryByName("vm")
        VirtualRouterVmVO vr = dbf.listAll(VirtualRouterVmVO.class).get(0)

        KVMAgentCommands.VmSyncCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_VM_SYNC_PATH){rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), KVMAgentCommands.VmSyncCmd.class)
            return rsp
        }
        reconnectHost {
            uuid = host1.uuid
        }
        retryInSecs {
            assert cmd != null
            assert dbf.listAll(VirtualRouterVmVO.class).size() == 1 && dbFindByUuid(vr.uuid,VmInstanceVO.class).state == VmInstanceState.Running
            assert dbFindByUuid(vmi.uuid, VmInstanceVO.class).getState() == VmInstanceState.Running
        }


        destroyVmInstance {
            uuid = vr.uuid
        }
        assert dbf.listAll(VirtualRouterVmVO.class).size() == 0


        KVMAgentCommands.StartVmCmd startVmCmd = new KVMAgentCommands.StartVmCmd()
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH){rsp, HttpEntity<String> entity ->
            startVmCmd = json(entity.getBody(), KVMAgentCommands.StartVmCmd.class)
            return rsp
        }
        rebootVmInstance {
            uuid = vmi.uuid
        }
        retryInSecs {
            assert startVmCmd.getVmInstanceUuid() == vmi.uuid
            assert dbf.listAll(VirtualRouterVmVO.class).size() == 1 && dbf.listAll(VirtualRouterVmVO.class).get(0).state == VmInstanceState.Running
        }
    }
    @Override
    void clean() {
        env.delete()
    }
}
