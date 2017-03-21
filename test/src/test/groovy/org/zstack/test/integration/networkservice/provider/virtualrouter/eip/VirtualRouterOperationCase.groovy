package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.DatabaseFacade
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.*

/**
 * Created by Camile on 2017/3/21
 */
class VirtualRouterOperationCase extends SubCase {
    EnvSpec env


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

    @Override
    void test() {
        env.create {

            testVirtualRouterReconnect()
        }
    }
}
