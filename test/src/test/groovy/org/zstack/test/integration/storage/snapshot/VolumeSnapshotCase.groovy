package org.zstack.test.integration.storage.snapshot

import org.zstack.sdk.CreateVolumeSnapshotSchedulerAction
import org.zstack.sdk.SchedulerInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec

import java.sql.Timestamp
import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/3/7.
 */
class VolumeSnapshotCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testSnapshotScheduleCase()
            testSnapshotScheduleJobTimeOutOfRange()
            testSnapshotScheduleJobPassiveStopTime()
            testSnapshotSchedulerJobWithoutSetInterval()
            testSnapshotSchedulerJobWithoutSetRepeatCount()
        }
    }

    void testSnapshotScheduleCase() {
        VmSpec vmSpec = env.specByName("vm")
        CreateVolumeSnapshotSchedulerAction action = new CreateVolumeSnapshotSchedulerAction()
        action.volumeUuid = vmSpec.inventory.rootVolumeUuid
        action.snapShotName = "test"
        action.schedulerName = "test"
        action.type = "simple"
        action.interval = 3600
        action.repeatCount = 100
        action.sessionId = adminSession()
        action.startTime = 3600
        CreateVolumeSnapshotSchedulerAction.Result result = action.call()
        TimeUnit.SECONDS.sleep(3)

        def startTime = result.value.inventory.startTime
        def stop =  result.value.inventory.stopTime

        assert stop.getTime() - startTime.getTime() == 3600 * 100 * 1000

        deleteScheduler {
            uuid = result.value.inventory.uuid
        }
        TimeUnit.SECONDS.sleep(3)
    }

    void testSnapshotScheduleJobTimeOutOfRange() {
        VmSpec vmSpec = env.specByName("vm")
        // schedule job time out of mysql timestamp range
        CreateVolumeSnapshotSchedulerAction action = new CreateVolumeSnapshotSchedulerAction()
        action.volumeUuid = vmSpec.inventory.rootVolumeUuid
        action.snapShotName = "test2"
        action.schedulerName = "test2"
        action.type = "simple"
        action.interval = Integer.MAX_VALUE
        action.repeatCount = 1000
        action.sessionId = adminSession()
        action.startTime = 3600
        CreateVolumeSnapshotSchedulerAction.Result result = action.call()

        assert result.error != null
    }

    void testSnapshotScheduleJobPassiveStopTime() {
        VmSpec vmSpec = env.specByName("vm")
        // schedule job duration time out of range
        CreateVolumeSnapshotSchedulerAction action = new CreateVolumeSnapshotSchedulerAction()
        action.volumeUuid = vmSpec.inventory.rootVolumeUuid
        action.snapShotName = "test3"
        action.schedulerName = "test3"
        action.type = "simple"
        action.interval = Integer.MAX_VALUE
        action.repeatCount = Integer.MAX_VALUE
        action.sessionId = adminSession()
        action.startTime = 2147454847 - 1
        CreateVolumeSnapshotSchedulerAction.Result result = action.call()

        assert result.error != null
    }

    void testSnapshotSchedulerJobWithoutSetInterval() {
        VmSpec vmSpec = env.specByName("vm")
        // schedule job without set interval
        SchedulerInventory schedulerInventory = createVolumeSnapshotScheduler {
            volumeUuid = vmSpec.inventory.rootVolumeUuid
            snapShotName = "test4"
            schedulerName = "test4"
            type = "simple"
            repeatCount = 1
            startTime = 0
        }

        assert schedulerInventory.repeatCount == 1
        assert schedulerInventory.schedulerName == "test4"
    }

    void testSnapshotSchedulerJobWithoutSetRepeatCount() {
        VmSpec vmSpec = env.specByName("vm")
        // forever job
        SchedulerInventory schedulerInventory = createVolumeSnapshotScheduler {
            volumeUuid = vmSpec.inventory.rootVolumeUuid
            snapShotName = "test5"
            schedulerName = "test5"
            type = "simple"
            interval = 3600
            startTime = 3600
        }

        assert schedulerInventory.stopTime == null
        assert schedulerInventory.repeatCount == null
    }
}
