package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.EipInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.*
import org.zstack.utils.CollectionUtils
import org.zstack.utils.function.Function

import javax.persistence.Tuple

/**
 * Created by xing5 on 2017/3/7.
 */
class VirtualRouterEipCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory publicL3
    EipInventory eip
    EipInventory eip1

    @Override
    void clean() {
        env.cleanSimulatorHandlers();
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
        VirtualRouterVmInventory vrvm = queryVirtualRouterVm {}[0]
        VirtualRouterCommands.CreateEipCmd cmd = new VirtualRouterCommands.CreateEipCmd()
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_EIP) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), VirtualRouterCommands.CreateEipCmd)
            return rsp
        }
        attachEip {
            eipUuid = eip1.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }
        assert cmd.eip.publicMac.equals(vrvm.vmNics.stream().filter{ nic ->
            nic.l3NetworkUuid.equals(publicL3.uuid)
        }.findFirst().get().mac)

        detachEip {
            uuid = eip1.uuid
        }

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

    void testVirtualRouterDHCP() {
        VmNicInventory defaultNic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid()) ? arg : null
            }
        })
        assert defaultNic != null

        final List<String> l3s = CollectionUtils.transformToList(vm.getVmNics(), new Function<String, VmNicInventory>() {
            String call(VmNicInventory arg) {
                return arg.getL3NetworkUuid()
            }
        })
        long count = Q.New(NetworkServiceL3NetworkRefVO.class).select()
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, "DHCP")
                .in(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3s).count()
        assert count == 1

        Tuple tuple = Q.New(VmNicVO.class).select(VmNicVO_.mac, VmNicVO_.ip).eq(VmNicVO_.uuid, defaultNic.uuid).findTuple()
        assert defaultNic.mac == tuple.get(0, String.class)
        assert defaultNic.ip == tuple.get(1, String.class)
    }

    void testDeleteEipOnRevokeEipFailure() {
        startVmInstance {
            uuid = vm.uuid
        }

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_EIP) {
            rsp, HttpEntity<String> e -> throw new CloudRuntimeException("injected fault")
        }

        // TODO: once we add GC to eip, the delete will success
        // then, fix the case
        expect(AssertionError.class) {
            deleteEip {
                uuid = eip.uuid
            }
        }

        assert dbIsExists(eip.uuid, EipVO.class)
        env.cleanAfterSimulatorHandlers()
    }

    @Override
    void test() {
        env.create {
            vm = (env.specByName("vm") as VmSpec).inventory
            eip = (env.specByName("eip") as EipSpec).inventory
            eip1 = (env.specByName("eip1") as EipSpec).inventory
            publicL3 = (env.specByName("pubL3") as L3NetworkSpec).inventory
            testDetachEipJustAfterAttachToStoppedVm()
            testDeleteEipOnRevokeEipFailure()
            testVirtualRouterDHCP()
        }
    }

}