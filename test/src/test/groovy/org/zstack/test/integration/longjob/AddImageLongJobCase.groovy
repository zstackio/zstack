package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.config.GlobalConfigVO
import org.zstack.core.config.GlobalConfigVO_
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.timeout.ApiTimeoutGlobalProperty
import org.zstack.header.image.APIAddImageMsg
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.image.ImageVO
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.header.longjob.LongJobState
import org.zstack.header.storage.backup.DownloadImageMsg
import org.zstack.header.storage.backup.DownloadImageReply
import org.zstack.header.storage.primary.APIAttachPrimaryStorageToClusterMsg
import org.zstack.longjob.LongJobGlobalConfig
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.SizeUtils
import org.zstack.utils.TimeUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import javax.persistence.metamodel.SingularAttribute

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
            testAddImageTimeout()
            testUpdateLongJobApiTimeout()
            testLongJobTimeout()
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
                jobName = msg.getClass().getSimpleName()
                jobData = gson.toJson(msg)
            }
        }
    }

    void testAddImage() {
        int oldSize = Q.New(ImageVO.class).list().size()
        int flag = 0
        myDescription = "my-test"

        def timeout = 0
        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)

            timeout = cmd.getTimeout()
            //DownloadImageMsg
            LongJobVO vo = Q.New(LongJobVO.class).eq(LongJobVO_.description, myDescription).find()
            assert vo.state == LongJobState.Running
            flag += 1
            return rsp
        }

        APIAddImageMsg msg = new APIAddImageMsg()
        msg.setName("TinyLinux")
        msg.setBackupStorageUuids(Collections.singletonList(bs.uuid))
        msg.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2")
        msg.setFormat(ImageConstant.QCOW2_FORMAT_STRING)
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())
        msg.setPlatform(ImagePlatform.Linux.toString())

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        assert jobInv.getJobName() == msg.getClass().getSimpleName()
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        int newSize = Q.New(ImageVO.class).count().intValue()
        assert newSize > oldSize
        assert 1 == flag
        assert (long) timeout <= TimeUtils.parseTimeInMillis("72h")
        assert (long) timeout + TimeUtils.parseTimeInMillis("1m") > TimeUtils.parseTimeInMillis("72h")

        env.cleanAfterSimulatorHandlers()
    }

    void testAddImageTimeout() {
        String uuid = Platform.uuid

        def timeout = 0
        env.message(DownloadImageMsg.class) { DownloadImageMsg dmsg, CloudBus bus ->
            timeout = dmsg.getTimeout()

            def reply = new DownloadImageReply()
            reply.setSize(SizeUnit.GIGABYTE.toByte(8))
            reply.setActualSize(SizeUnit.GIGABYTE.toByte(8))
            reply.setFormat("qcow2")
            reply.setInstallPath("test/test")
            reply.setMd5sum("testmd5")

            bus.reply(dmsg, reply)
        }

        addImage {
            name = "test2"
            url = "http://192.168.1.20/share/images/test2.qcow2"
            backupStorageUuids = Collections.singletonList(bs.uuid)
            format = ImageConstant.RAW_FORMAT_STRING
            mediaType = ImageConstant.ImageMediaType.RootVolumeTemplate.toString()
            platform = ImagePlatform.Linux.toString()
            resourceUuid = uuid
        }

        assert null != dbFindByUuid(uuid, ImageVO.class)
        // timeout should be 3h from global property
        assert timeout <= TimeUtils.parseTimeInMillis("72h")
        assert timeout + TimeUtils.parseTimeInMillis("1m") > TimeUtils.parseTimeInMillis("72h")

        env.cleanMessageHandlers()
    }

    void testAddImageAppointResourceUuid() {
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

        def timeout = 0
        env.message(DownloadImageMsg.class) { DownloadImageMsg dmsg, CloudBus bus ->
            timeout = dmsg.getTimeout()

            def reply = new DownloadImageReply()
            reply.setSize(SizeUnit.GIGABYTE.toByte(8))
            reply.setActualSize(SizeUnit.GIGABYTE.toByte(8))
            reply.setFormat("qcow2")
            reply.setInstallPath("test/test")
            reply.setMd5sum("testmd5")

            bus.reply(dmsg, reply)
        }

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        assert jobInv.getJobName() == msg.getClass().getSimpleName()
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }

        assert null != dbFindByUuid(uuid, ImageVO.class)
        assert (long) timeout <= TimeUtils.parseTimeInMillis("72h")
        assert (long) timeout + TimeUtils.parseTimeInMillis("1m") > TimeUtils.parseTimeInMillis("72h")

        env.cleanMessageHandlers()
    }

    void testUpdateLongJobApiTimeout() {
        myDescription = "my-test4"

        String uuid = Platform.uuid

        def action = new UpdateGlobalConfigAction()
        action.category = LongJobGlobalConfig.CATEGORY
        action.name = LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.name
        action.value = 10799
        action.sessionId = adminSession()
        UpdateGlobalConfigAction.Result ret = action.call()

        assert ret.error != null

        updateGlobalConfig {
            category = LongJobGlobalConfig.CATEGORY
            name = LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.name
            value = 38000
        }

        APIAddImageMsg msg = new APIAddImageMsg()
        msg.setName("TinyLinux")
        msg.setBackupStorageUuids(Collections.singletonList(bs.uuid))
        msg.setUrl("http://192.168.1.20/share/images/testestestestest.qcow2")
        msg.setFormat(ImageConstant.RAW_FORMAT_STRING)
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())
        msg.setPlatform(ImagePlatform.Linux.toString())
        msg.setResourceUuid(uuid)

        def timeout = 0
        env.message(DownloadImageMsg.class) { DownloadImageMsg dmsg, CloudBus bus ->
            timeout = dmsg.getTimeout()

            def reply = new DownloadImageReply()
            reply.setSize(SizeUnit.GIGABYTE.toByte(8))
            reply.setActualSize(SizeUnit.GIGABYTE.toByte(8))
            reply.setFormat("qcow2")
            reply.setInstallPath("test/test")
            reply.setMd5sum("testmd5")

            bus.reply(dmsg, reply)
        }

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        assert jobInv.getJobName() == msg.getClass().getSimpleName()
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }

        assert null != dbFindByUuid(uuid, ImageVO.class)
        assert timeout <= 259200000
        assert timeout + TimeUtils.parseTimeInMillis("1m") > 259200000

        env.cleanMessageHandlers()
    }

    void testLongJobTimeout() {
        myDescription = "my-test5"

        String uuid = Platform.uuid

        ApiTimeoutGlobalProperty.MINIMAL_TIMEOUT = "0"
        updateGlobalConfig {
            category = "apiTimeout"
            name = APIAddImageMsg.class.getName()
            value = "1"
        }

        APIAddImageMsg msg = new APIAddImageMsg()
        msg.setName("TinyLinux")
        msg.setBackupStorageUuids(Collections.singletonList(bs.uuid))
        msg.setUrl("http://192.168.1.20/share/images/222.qcow2")
        msg.setFormat(ImageConstant.RAW_FORMAT_STRING)
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())
        msg.setPlatform(ImagePlatform.Linux.toString())
        msg.setResourceUuid(uuid)

        def timeout = 0
        env.message(DownloadImageMsg.class) { DownloadImageMsg dmsg, CloudBus bus ->
            timeout = dmsg.getTimeout()

            sleep(100)

            def reply = new DownloadImageReply()
            reply.setSize(SizeUnit.GIGABYTE.toByte(8))
            reply.setActualSize(SizeUnit.GIGABYTE.toByte(8))
            reply.setFormat("qcow2")
            reply.setInstallPath("test/test")
            reply.setMd5sum("testmd5")

            bus.reply(dmsg, reply)
        }

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = myDescription
        } as LongJobInventory

        assert jobInv.getJobName() == msg.getClass().getSimpleName()
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        // confirm long job timeout
        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Failed.toString()
        }
        env.cleanMessageHandlers()
        ApiTimeoutGlobalProperty.MINIMAL_TIMEOUT = "5m"
    }
}
