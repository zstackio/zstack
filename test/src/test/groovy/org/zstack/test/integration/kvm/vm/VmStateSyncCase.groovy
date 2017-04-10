package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.ReconnectHostAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

/**
 * Created by AlanJager on 2017/4/7.
 */
class VmStateSyncCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testVmStateSyncOnCephPrimaryStorage()
        }
    }

    void testVmStateSyncOnCephPrimaryStorage() {
        HostInventory hostInventory = env.inventoryByName("kvm")
        VmInstanceInventory vmInstanceInventory = env.inventoryByName("vm")

        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.RefreshAllRulesOnHostCmd.class)
            def sgRsp = new KVMAgentCommands.ApplySecurityGroupRuleResponse()
            sgRsp.success = true
            return sgRsp
        }

        ReconnectHostAction action = new ReconnectHostAction()
        action.uuid = hostInventory.uuid
        action.sessionId = adminSession()
        ReconnectHostAction.Result ret = action.call()
        assert ret.error != null

        assert dbFindByUuid(vmInstanceInventory.uuid, VmInstanceVO.class).state == VmInstanceState.Unknown

        destroyVmInstance {
            uuid = vmInstanceInventory.uuid
        }

        recoverVmInstance {
            uuid = vmInstanceInventory.uuid
        }
        env.cleanSimulatorHandlers()

        reconnectHost {
            uuid = hostInventory.uuid
        }

        assert dbFindByUuid(vmInstanceInventory.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
    }
}
