package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.zstack.core.db.Q
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg
import org.zstack.header.image.ImageVO
import org.zstack.header.longjob.LongJobVO
import org.zstack.sdk.*
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by camile on 2018/3/8.
 */
class CreateDataVolumeTemplateLongJobCase extends SubCase {
    EnvSpec env
    Gson gson
    VolumeInventory dataVolume
    BackupStorageInventory bs

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnvForPrimaryStorage()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            testCreateDataVolumeTemplateFailure()
            testCreateDataVolumeTemplateSuccess()
        }
    }

    void testCreateDataVolumeTemplateFailure() {
        gson = new Gson()
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        bs = env.inventoryByName("sftp") as BackupStorageInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory
        def vm = env.inventoryByName("test-vm") as VmInstanceInventory

        long oldCount = Q.New(ImageVO.class).count()

        dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = ps.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
        } as VolumeInventory

        APICreateDataVolumeTemplateFromVolumeMsg badMsg = new APICreateDataVolumeTemplateFromVolumeMsg()
        badMsg.name = "data-volume"
        badMsg.volumeUuid = dataVolume.uuid + 1
        badMsg.backupStorageUuids = [bs.uuid]

        expect(ApiException.class) {
            submitLongJob {
                submitLongJob {
                    jobName = badMsg.getClass().getSimpleName()
                    jobData = gson.toJson(badMsg)
                }
            }
        }

        assert oldCount == Q.New(ImageVO.class).count()
    }

    void testCreateDataVolumeTemplateSuccess() {
        long oldCount = Q.New(ImageVO.class).count()

        APICreateDataVolumeTemplateFromVolumeMsg msg = new APICreateDataVolumeTemplateFromVolumeMsg()
        msg.name = "data-volume"
        msg.volumeUuid = dataVolume.uuid
        msg.backupStorageUuids = [bs.uuid]

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state.toString() == LongJobState.Running.toString()

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }

        assert oldCount < Q.New(ImageVO.class).count()
    }
}