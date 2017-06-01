package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.vm.VmNicInventory
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L2NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by weiwang on 01/06/2017.
 */
class VirtualRouterAttachNetworkCase extends SubCase {

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
        env.create {
            testAttachOneSystemNetwork()
        }
    }

    void testAttachOneSystemNetwork() {
        def l2 = env.inventoryByName("l2") as L2NetworkInventory

        L3NetworkInventory l3 = createL3Network {
            delegate.system = true
            delegate.l2NetworkUuid = l2.uuid
            delegate.name = "pubL3-2"
        }

        addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = l3.uuid
            delegate.startIp = "11.168.200.10"
            delegate.endIp = "11.168.200.253"
            delegate.gateway = "11.168.200.1"
            delegate.netmask = "255.255.255.0"
        }

        VirtualRouterVmVO vr = Q.New(VirtualRouterVmVO.class).find()

        VirtualRouterCommands.ConfigureNicCmd cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CONFIGURE_NIC_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.ConfigureNicCmd.class)
            return rsp
        }

        VmInstanceInventory inv = attachL3NetworkToVm {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.vmInstanceUuid = vr.uuid
        }

        assert cmd != null

        VirtualRouterCommands.RemoveNicCmd rcmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_NIC_PATH) { rsp, HttpEntity<String> e ->
            rcmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.RemoveNicCmd.class)
            return rsp
        }

        detachL3NetworkFromVm {
            delegate.vmNicUuid = (inv.vmNics.stream()
                    .filter{nic -> l3.getUuid().equals(nic.l3NetworkUuid)}.find() as org.zstack.sdk.VmNicInventory).uuid
        }

        assert rcmd != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
