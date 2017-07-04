package org.zstack.test.integration.kvm.vm

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.allocator.AllocateHostMsg
import org.zstack.header.allocator.AllocateHostReply
import org.zstack.header.host.CheckVmStateOnHypervisorMsg
import org.zstack.header.host.CheckVmStateOnHypervisorReply
import org.zstack.header.network.l3.AllocateIpMsg
import org.zstack.header.network.l3.AllocateIpReply
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.StartVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import javax.persistence.Tuple

import static org.zstack.core.Platform.operr

/**
 * Created by MaJin on 2017-06-28.
 */
class VmLastHostUuidCase extends SubCase{
    EnvSpec env
    HostInventory host1, host2
    VmInstanceInventory vm

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmTwoHostNfsEnv()
    }

    @Override
    void test() {
        env.create {
            host1 = env.inventoryByName("host1") as HostInventory
            host2 = env.inventoryByName("host2") as HostInventory
            vm = env.inventoryByName("vm") as VmInstanceInventory // vm is in host1 now
            testStartVmHostUuid()
            testStartVmHypervisorFailHostUuid()
            testStartVmAllocateHostFailHostUuid()
            testMigrateVmHostUuid()
            testMigrateVmHypervisorFailHostUuid()
            testMigrateVmAllocateHostFailHostUuid()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    private testStartVmExpect(boolean expect, String startVmHostUuid, String expectHostUuid, String expectLastHostUuid){
        StartVmInstanceAction a = new StartVmInstanceAction()
        a.uuid = vm.uuid
        a.hostUuid = startVmHostUuid
        a.sessionId = currentEnvSpec.session.uuid
        def ret = a.call()
        if(expect){
            assert ret.error == null
        }else {
            assert ret.error != null
        }

        Tuple t = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                .findTuple()
        String hostUuid = t.get(0, String.class)
        String lastHostUuid = t.get(1, String.class)
        assert hostUuid == expectHostUuid
        assert lastHostUuid == expectLastHostUuid
    }

    private testMigrateVmExpect(boolean expect, String migrateVmHostUuid, String expectHostUuid, String expectLastHostUuid){
        env.message(CheckVmStateOnHypervisorMsg.class) { CheckVmStateOnHypervisorMsg msg, CloudBus bus ->
            def reply = new CheckVmStateOnHypervisorReply()
            def list = new HashMap<String, String>()
            list.put(vm.uuid, VmInstanceState.Running.toString())
            reply.setStates(list)
            reply.success = true
            bus.reply(msg, reply)
        }

        MigrateVmAction a = new MigrateVmAction()
        a.vmInstanceUuid = vm.uuid
        a.hostUuid = migrateVmHostUuid
        a.sessionId = currentEnvSpec.session.uuid
        def ret = a.call()
        if(expect){
            assert ret.error == null
        }else {
            assert a.call().error != null
        }

        Tuple t = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                .findTuple()
        String hostUuid = t.get(0, String.class)
        String lastHostUuid = t.get(1, String.class)
        assert hostUuid == expectHostUuid
        assert lastHostUuid == expectLastHostUuid
    }

    void testStartVmHostUuid(){
        stopVmInstance {
            uuid = vm.uuid
        }

        Tuple t = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                .findTuple()
        String hostUuid = t.get(0, String.class)
        String lastHostUuid = t.get(1, String.class)
        assert hostUuid == null
        assert lastHostUuid == host1.uuid

        testStartVmExpect(true, host2.uuid, host2.uuid, host1.uuid)
    }

    void testStartVmHypervisorFailHostUuid(){
        stopVmInstance {
            uuid = vm.uuid
        }

        env.simulator(KVMConstant.KVM_START_VM_PATH) {
            def rsp = new KVMAgentCommands.StartVmResponse()
            rsp.setError("fail to start vm")
            return rsp
        }
        env.message(CheckVmStateOnHypervisorMsg.class) { CheckVmStateOnHypervisorMsg msg, CloudBus bus ->
            def reply = new CheckVmStateOnHypervisorReply()
            def list = new HashMap<String, String>()
            list.put(vm.uuid, VmInstanceState.Stopped.toString())
            reply.setStates(list)
            reply.success = true
            bus.reply(msg, reply)
        }

        testStartVmExpect(false, host1.uuid, null, host2.uuid)

        env.cleanSimulatorAndMessageHandlers()
    }

    void testStartVmAllocateHostFailHostUuid(){
        env.message(AllocateHostMsg.class) { AllocateHostMsg msg, CloudBus bus ->
            def reply = new AllocateHostReply()
            reply.setError(operr("allocate host fail"))
            bus.reply(msg, reply)
        }
        testStartVmExpect(false, host1.uuid, null, host2.uuid)

        env.cleanSimulatorAndMessageHandlers()
    }

    void testMigrateVmHostUuid(){
        startVmInstance {
            uuid = vm.uuid
            hostUuid = host1.uuid
        }
        testMigrateVmExpect(true, host2.uuid, host2.uuid, host1.uuid)
    }

    void testMigrateVmHypervisorFailHostUuid(){
        env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) {
            def rsp = new KVMAgentCommands.MigrateVmResponse()
            rsp.setError("migrate fail")
            return rsp
        }
        testMigrateVmExpect(false, host1.uuid, host2.uuid, host1.uuid)

        env.cleanSimulatorAndMessageHandlers()
    }

    void testMigrateVmAllocateHostFailHostUuid(){
        env.message(AllocateHostMsg.class) { AllocateHostMsg msg, CloudBus bus ->
            def reply = new AllocateHostReply()
            reply.setError(operr("allocate host fail"))
            bus.reply(msg, reply)
        }
        testMigrateVmExpect(false, host1.uuid, host2.uuid, host1.uuid)

        env.cleanSimulatorAndMessageHandlers()
    }


}
