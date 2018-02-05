package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.image.APIAddImageMsg
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.image.ImageVO
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.header.longjob.LongJobState
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by camile on 2/5/18.
 */
class AddImageLongJobCase extends SubCase {
    EnvSpec env
    Gson gson
    BackupStorageInventory bs
    String myDescription

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testApiMessageValidator()
            testAddImage()
            testAddImageAppointResourceUuid()
        }
    }

    void testApiMessageValidator() {
        bs = env.inventoryByName("sftp") as BackupStorageInventory
        gson = new Gson()

        APIAddImageMsg msg = new APIAddImageMsg()
        msg.setName("TinyLinux")
        msg.setBackupStorageUuids(Collections.singletonList(bs.uuid))
        msg.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2")
        msg.setFormat(ImageConstant.QCOW2_FORMAT_STRING)
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())
        msg.setPlatform(ImagePlatform.Linux.toString() + 1)

        expect(AssertionError.class) {
            submitLongJob {
                sessionId = adminSession()
                jobName = "APIAddImageMsg"
                jobData = gson.toJson(msg)
            }
        }
    }

    void testAddImage() {
        int oldSize = Q.New(ImageVO.class).list().size()
        int flag = 0
        myDescription = "my-test"

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { Object response ->
            //DownloadImageMsg
            LongJobVO vo = Q.New(LongJobVO.class).eq(LongJobVO_.description, myDescription).find()
            assert vo.state == LongJobState.Running
            flag += 1
            return response
        }

        APIAddImageMsg msg = new APIAddImageMsg()
        msg.setName("TinyLinux")
        msg.setBackupStorageUuids(Collections.singletonList(bs.uuid))
        msg.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2")
        msg.setFormat(ImageConstant.QCOW2_FORMAT_STRING)
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())
        msg.setPlatform(ImagePlatform.Linux.toString())

        LongJobInventory jobInv = submitLongJob {
            sessionId = adminSession()
            jobName = "APIAddImageMsg"
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        assert jobInv.getJobName() == "APIAddImageMsg"
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        int newSize = Q.New(ImageVO.class).count().intValue()
        assert newSize > oldSize
        assert 1 == flag
    }

    void testAddImageAppointResourceUuid(){
        myDescription = "my-test3"

        String uuid = Platform.uuid
        APIAddImageMsg msg = new APIAddImageMsg()
        msg.setName("TinyLinux")
        msg.setBackupStorageUuids(Collections.singletonList(bs.uuid))
        msg.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2")
        msg.setFormat(ImageConstant.RAW_FORMAT_STRING)
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())
        msg.setPlatform(ImagePlatform.Linux.toString())
        msg.setResourceUuid(uuid)

        LongJobInventory jobInv = submitLongJob {
            sessionId = adminSession()
            jobName = "APIAddImageMsg"
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        assert jobInv.getJobName() == "APIAddImageMsg"
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        assert null != dbFindByUuid(uuid,ImageVO.class)
    }
}
