package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceEO
import org.zstack.header.vm.VmInstanceEO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vo.ResourceVO
import org.zstack.header.vo.ResourceVO_
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.resourceconfig.ResourceConfigVO
import org.zstack.resourceconfig.ResourceConfigVO_
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

        reconnectHost {
            uuid = host1.uuid
        }
        retryInSecs {
            assert dbf.listAll(VirtualRouterVmVO.class).size() == 1 && dbFindByUuid(vr.uuid,VmInstanceVO.class).state == VmInstanceState.Running
        }

        destroyVmInstance {
            uuid = vr.uuid
        }
        assert dbf.listAll(VirtualRouterVmVO.class).size() == 0
        assert !Q.New(VmInstanceEO.class).eq(VmInstanceEO_.uuid, vr.uuid).exists
        assert !Q.New(ResourceVO.class).eq(ResourceVO_.uuid, vr.uuid).exists
        assert !Q.New(ResourceConfigVO.class).eq(ResourceConfigVO_.resourceUuid ,vr.uuid).exists

        rebootVmInstance {
            uuid = vmi.uuid
        }
        retryInSecs {
            assert dbf.listAll(VirtualRouterVmVO.class).size() == 1 && dbf.listAll(VirtualRouterVmVO.class).get(0).state == VmInstanceState.Running
        }
    }
    @Override
    void clean() {
        env.delete()
    }
}
