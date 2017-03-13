package org.zstack.test.integration.storage.snapshot

import org.zstack.sdk.CreateVolumeSnapshotSchedulerAction
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec

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

        def startTime = result.value.inventory.startTime
        def stop =  result.value.inventory.stopTime

        assert stop.getTime() - startTime.getTime() == 3600 * 100 * 1000
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
}
