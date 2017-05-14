package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.DatabaseFacade
import org.springframework.http.HttpEntity
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.core.db.Q
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_
import org.zstack.sdk.EipInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EipSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by Camile on 2017/3/24.
 */

class VirtualRouterOperationCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory publicL3
    EipInventory eip

    @Override
    void test() {
        env.create {
            vm = (env.specByName("vm") as VmSpec).inventory
            eip = (env.specByName("eip") as EipSpec).inventory
            publicL3 = (env.specByName("pubL3") as L3NetworkSpec).inventory

            testVirtualRouterReconnect()
            testUpdateGlobalConfigAndPingVirtualRouter()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
    }

    void testVirtualRouterReconnect() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0)
        ApplianceVmInventory inv = reconnectVirtualRouter {
            vmInstanceUuid = vr.uuid
            sessionId = adminSession()
        }
    }


    void testUpdateGlobalConfigAndPingVirtualRouter() {
        assert (Q.New(VirtualRouterVmVO.class).select(VirtualRouterVmVO_.uuid).listValues().get(0)) != null
        updateGlobalConfig {
            name = "ping.interval"
            category = "virtualRouter"
            value = 1
            sessionId = adminSession()
        }

        env.simulator(VirtualRouterConstant.VR_PING) { rsp, HttpEntity<String> e ->

            rsp.success = true
            return rsp
        }
        VirtualRouterCommands.InitCommand cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_INIT) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.InitCommand.class)
            return rsp
        }
        retryInSecs(5, 1) {
            assert cmd != null
        }
    }
}