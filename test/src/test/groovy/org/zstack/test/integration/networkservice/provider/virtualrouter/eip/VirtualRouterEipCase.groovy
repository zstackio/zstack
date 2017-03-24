package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.vip.VipVO
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

/**
 * Created by xing5 on 2017/3/7.
 */
class VirtualRouterEipCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory publicL3
    EipInventory eip

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

    void testDetachEipJustAfterAttachToStoppedVm() {
        stopVmInstance {
            uuid = vm.uuid
        }

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        detachEip {
            uuid = eip.uuid
        }

        EipVO vo = dbFindByUuid(eip.uuid, EipVO.class)
        assert vo.vmNicUuid == null

        VipVO vip = dbFindByUuid(eip.vipUuid, VipVO.class)
        // the vip has not created on backend
        assert vip.serviceProvider == null
        assert vip.useFor == EipConstant.EIP_NETWORK_SERVICE_TYPE
    }

    @Override
    void test() {
        env.create {
            vm = (env.specByName("vm") as VmSpec).inventory
            eip = (env.specByName("eip") as EipSpec).inventory
            publicL3 = (env.specByName("pubL3") as L3NetworkSpec).inventory

            testDetachEipJustAfterAttachToStoppedVm()
        }
    }
}
