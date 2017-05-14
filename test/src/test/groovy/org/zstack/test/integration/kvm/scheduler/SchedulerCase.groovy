package org.zstack.test.integration.kvm.scheduler


import org.zstack.core.db.Q
import org.zstack.header.core.scheduler.SchedulerVO
import org.zstack.header.core.scheduler.SchedulerVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.sdk.CreateStartVmInstanceSchedulerAction
import org.zstack.sdk.SchedulerInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec

/**
 * Created by Camile on 2017/3.
 */
class SchedulerCase extends SubCase {
    EnvSpec env

    def DOC = """  """

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
            testCreateSchedulerAndChangeStateSuccess()
            testReloadScheduler()
            testDeleteScheduler()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testCreateSchedulerAndChangeStateSuccess() {
        String vm_uuid = (env.specByName("vm") as VmSpec).inventory.uuid
        createRebootVmInstanceScheduler {
            vmUuid = vm_uuid
            schedulerName = "test"
            schedulerDescription = "reboot vm"
            type = "simple"
            startTime = (System.currentTimeMillis() / 1000) + 6000
            repeatCount = 1
        }
        SchedulerVO svo = Q.New(SchedulerVO.class).eq(SchedulerVO_.targetResourceUuid, vm_uuid).find()
        assert svo != null
        assert svo.state == "Enabled"

        changeSchedulerState {
            uuid = svo.uuid
            stateEvent = "disable"
        }
        SchedulerVO svo2 = Q.New(SchedulerVO.class).eq(SchedulerVO_.targetResourceUuid, vm_uuid).find()
        assert svo2 != null
        assert svo2.state == "Disabled"
        String state = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm_uuid).select(VmInstanceVO_.state).findValue()
        assert state == "Running"
    }

    void testReloadScheduler() {
        String vm_uuid = (env.specByName("vm") as VmSpec).inventory.uuid
        VmInstanceInventory inv = stopVmInstance {
            uuid = vm_uuid
        }
        String state = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm_uuid).select(VmInstanceVO_.state).findValue()
        assert state == "Stopped"

        createStartVmInstanceScheduler {
            vmUuid = vm_uuid
            schedulerName = "start vm"
            schedulerDescription = "description"
            type = "simple"
            startTime = 0
            repeatCount = 1
        }
        retryInSecs(20, 5) {
            String state2 = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm_uuid).select(VmInstanceVO_.state).findValue()
            assert state2 == "Running"
        }

    }
    void testDeleteScheduler() {
        String vm_uuid = (env.specByName("vm") as VmSpec).inventory.uuid
        VmInstanceInventory inv = stopVmInstance {
            uuid = vm_uuid
        }
        String state = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm_uuid).select(VmInstanceVO_.state).findValue()
        assert state == "Stopped"

        CreateStartVmInstanceSchedulerAction a = new CreateStartVmInstanceSchedulerAction()
        a.vmUuid = vm_uuid
        a.schedulerName = "start vm"
        a.schedulerDescription = "description"
        a.type = "simple"
        a.startTime = (System.currentTimeMillis() / 1000) + 5
        a.repeatCount = 1
        a.sessionId = adminSession()
        CreateStartVmInstanceSchedulerAction.Result res = a.call()
        assert res.error == null
        SchedulerInventory sInv = res.value.inventory
        deleteScheduler{
            uuid = sInv.uuid
        }
        retryInSecs(5, 1) {
            String state2 = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm_uuid).select(VmInstanceVO_.state).findValue()
            assert state2 == "Stopped"
        }

    }
}
