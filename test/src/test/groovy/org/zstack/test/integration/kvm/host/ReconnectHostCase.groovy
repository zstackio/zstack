package org.zstack.test.integration.kvm.host

import groovy.transform.Synchronized
import org.zstack.compute.vm.VmInstanceBase
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.host.HostStatus
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceStateEvent
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.VmStateChangedOnHostMsg
import org.zstack.header.vm.VmStateChangedOnHostReply
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

/**
 * Created by MaJin on 2017-05-01.
 */
class ReconnectHostCase extends SubCase{
    EnvSpec env
    private final static CLogger logger = Utils.getLogger(ReconnectHostCase.class)
    static RECONNECT_TIME = 10
    volatile int unknownVm, runVm, other

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
        env.create {
            testRreconnectHostVmState()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testRreconnectHostVmState(){
        VmInstanceInventory vmInv = env.inventoryByName("vm") as VmInstanceInventory
        int numberOfVm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.hostUuid, vmInv.hostUuid).count()
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        unknownVm = 0
        runVm = 0
        other = 0
        env.message(VmStateChangedOnHostMsg.class) { VmStateChangedOnHostMsg msg, CloudBus bus ->
            addTimes(msg.getStateOnHost())
            VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.vmInstanceUuid).find()
            vmvo.setState(VmInstanceState.valueOf(msg.stateOnHost))
            dbf.updateAndRefresh(vmvo)
            def reply = new VmStateChangedOnHostReply()
            bus.reply(msg, reply)
        }

        for (int i = 0; i < RECONNECT_TIME; i++) {
            reconnectHost {
                uuid = vmInv.hostUuid
                sessionId = currentEnvSpec.session.uuid
            }

            retryInSecs(){
                List<VmInstanceInventory> vmInvs = queryVmInstance {
                    conditions=["state=${VmInstanceState.Running}"]
                } as List<VmInstanceInventory>

                assert vmInvs.size() == 2
            }
            assert runVm == numberOfVm * (i + 1)
            assert unknownVm == numberOfVm * (i + 1)
        }

        assert runVm == numberOfVm * RECONNECT_TIME
        assert unknownVm == numberOfVm * RECONNECT_TIME
    }

    @Synchronized
    void addTimes(String status){
        int temp
        if(status == VmInstanceState.Running.toString()){
            temp = runVm
            runVm ++
            logger.debug(String.format("runVm add one: from:%d to %d", temp, runVm))
        }else if(status == VmInstanceState.Unknown.toString()){
            temp = unknownVm
            unknownVm ++
            logger.debug(String.format("unknownVm add one: from:%d to %d", temp, unknownVm))
        }else {
            other ++
        }
    }
}
