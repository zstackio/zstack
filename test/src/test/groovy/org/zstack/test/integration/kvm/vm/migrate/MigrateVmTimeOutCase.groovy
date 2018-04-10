package org.zstack.test.integration.kvm.vm.migrate

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.asyncbatch.While
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.db.Q
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.Constants
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.allocator.HostCapacityVO_
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.core.workflow.WhileCompletion
import org.zstack.header.host.HostConstant
import org.zstack.header.host.MigrateVmOnHypervisorMsg
import org.zstack.header.host.PingHostMsg
import org.zstack.header.message.MessageReply
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.ApiException
import org.zstack.sdk.HostInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import javax.persistence.Tuple
import java.util.concurrent.TimeUnit

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by MaJin on 2018/3/4.
 */
class MigrateVmTimeOutCase extends SubCase{
    EnvSpec env
    HostInventory host1, host2
    VmInstanceInventory vm
    InstanceOfferingInventory instance
    ErrorFacade errf
    RESTFacade restf
    CloudBus bus
    KVMConstant.KvmVmState vmStateOnHost1, vmStateOnHost2

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = VmMigrateEnv.oneVmThreeHostsNfsStorage()
    }

    @Override
    void test() {
        env.create {
            host1 = env.inventoryByName("kvm") as HostInventory
            host2 = env.inventoryByName("kvm1") as HostInventory
            vm = env.inventoryByName("vm") as VmInstanceInventory
            instance = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            errf = bean(ErrorFacade.class)
            restf = bean(RESTFacade.class)
            bus = bean(CloudBus.class)
            simulatorEnv()
            testMigrateVmTimeOut()
        }
    }

    @Override
    void clean() {
        KVMGlobalConfig.VM_SYNC_ON_HOST_PING.updateValue(false)
        env.delete()
    }

    void testMigrateVmTimeOut() {
        env.message(MigrateVmOnHypervisorMsg.class) { MigrateVmOnHypervisorMsg msg, CloudBus bus ->
            def r = new MessageReply()
            r.setError(errf.stringToTimeoutError("on purpose"))
            bus.reply(msg, r)
        }

        expect(AssertionError) {
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = host2.uuid
            }
        }

        VmInstanceVO nowVm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).find()
        assert nowVm.state == VmInstanceState.Running
        assert nowVm.hostUuid == host1.uuid

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        vmStateOnHost2 = KVMConstant.KvmVmState.Paused
        syncVmState([host2.uuid])

        1.upto(3) {
            sleep(100)
            assert vmState(vm.uuid) == VmInstanceState.Running
        }

        vmStateOnHost1 = KVMConstant.KvmVmState.Shutdown
        vmStateOnHost2 = KVMConstant.KvmVmState.Paused
        syncVmState([host1.uuid, host2.uuid])

        retryInSecs{
            assert vmState(vm.uuid) == VmInstanceState.Stopped
        }

        if (cmd == null) {
            syncVmState([host2.uuid])
            retryInSecs {
                assert cmd != null
            }
        }
    }

    private void syncVmState(List<String> hostUuids){
        FutureCompletion future = new FutureCompletion(null)

        new While<>(hostUuids).all({String huuid, WhileCompletion completion ->
            def msg = new PingHostMsg()
            msg.setHostUuid(huuid)
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid)
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                void run(MessageReply reply) {
                    completion.done()
                }
            })
        }).run(new NoErrorCompletion(){
            @Override
            void done() {
                future.success()
            }
        })

        future.await(TimeUnit.SECONDS.toMillis(10))
    }

    private void simulatorEnv(){
        env.simulator(KVMConstant.KVM_VM_SYNC_PATH) { HttpEntity<String> e ->
            def hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            List<Tuple> states = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.uuid, VmInstanceVO_.state)
                    .in(VmInstanceVO_.state, [VmInstanceState.Running, VmInstanceState.Unknown])
                    .eq(VmInstanceVO_.hostUuid, hostUuid).listTuple()

            def rsp = new KVMAgentCommands.VmSyncResponse()
            rsp.states = [:]
            states.each {
                String vmUuid = it.get(0, String.class)
                VmInstanceState state = it.get(1, VmInstanceState.class)
                if (state == VmInstanceState.Unknown) {
                    // host reconnecting will set VMs to Unknown in DB
                    // the spec.simulator treat them as Running by default
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.Running.toString()
                } else {
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.fromVmInstanceState(state).toString()
                }
            }

            if (vmStateOnHost1 != null && hostUuid == host1.uuid) {
                rsp.states[(vm.uuid)] = vmStateOnHost1.toString()
            }

            if (vmStateOnHost2 != null && hostUuid == host2.uuid) {
                rsp.states[(vm.uuid)] = vmStateOnHost2.toString()
            }

            return rsp
        }

        KVMGlobalConfig.VM_SYNC_ON_HOST_PING.updateValue(true)
    }

    final private static VmInstanceState vmState(String vmUuid){
        return Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.state).findValue()
    }
}
