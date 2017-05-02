package org.zstack.test.integration.kvm.host

import junit.framework.Assert
import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.header.host.HostState
import org.zstack.header.host.HostStateEvent
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceStateEvent
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ChangeHostStateAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.simulator.kvm.KVMSimulatorConfig
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.TimeUnit

/**
 * Created by heathhose on 17-4-7.
 */
class ChangeDisconncetedHostToMaintainCase extends SubCase{

    EnvSpec env
    DatabaseFacade dbf
    HostInventory host
    VmInstanceInventory vm
    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            host = env.inventoryByName("kvm")
            vm = env.inventoryByName("vm")
            testChangeStateIntoMaintainOneVMUnkown()
        }

    }

    void testChangeStateIntoMaintainOneVMUnkown(){
        VmInstanceInventory vm = env.inventoryByName("vm")
        assert vm.state == VmInstanceState.Running.name()

        env.simulator(KVMConstant.KVM_CONNECT_PATH){HttpEntity<String> entity,EnvSpec spec ->
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = false
            return rsp
        }

        expect(AssertionError.class){
            reconnectHost {
                uuid = host.uuid
            }
        }
        retryInSecs {
            HostVO hvo = dbf.findByUuid(host.getUuid(), HostVO.class)
            assert HostStatus.Disconnected == hvo.getStatus()
            VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class)
            assert VmInstanceState.Unknown == vmvo.getState()
        }

        ChangeHostStateAction action = new ChangeHostStateAction()
        action.uuid = host.uuid
        action.stateEvent = HostStateEvent.maintain
        action.sessionId = adminSession()
        ChangeHostStateAction.Result result = action.call()
        assert result.error != null
        assert dbFindByUuid(host.uuid,HostVO.class).state == HostState.Enabled

    }
    
    @Override
    void clean() {
        env.delete()
    }
}
