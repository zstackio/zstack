package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstance
import org.zstack.header.vm.VmInstanceEO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

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
        TimeUnit.SECONDS.sleep(2);
        assert dbf.listAll(VirtualRouterVmVO.class).size() == 1
        assert dbFindByUuid(vr.uuid,VmInstanceVO.class).state == VmInstanceState.Running

        destroyVmInstance {
            uuid = vr.uuid
        }
        assert dbf.listAll(VirtualRouterVmVO.class).size() == 0
        rebootVmInstance {
            uuid = vmi.uuid
        }
        TimeUnit.SECONDS.sleep(2);
        vr = dbf.listAll(VirtualRouterVmVO.class).get(0)
        assert dbFindByUuid(vr.uuid,VmInstanceVO.class).state == VmInstanceState.Running
    }
    @Override
    void clean() {
        env.delete()
    }
}
