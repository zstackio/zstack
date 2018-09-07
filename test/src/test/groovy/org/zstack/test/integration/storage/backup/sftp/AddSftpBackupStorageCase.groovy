package org.zstack.test.integration.storage.backup.sftp

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.image.ImageBackupStorageRefVO
import org.zstack.header.image.ImageBackupStorageRefVO_
import org.zstack.sdk.AddSftpBackupStorageAction
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 2017/4
 */
class AddSftpBackupStorageCase extends SubCase {

    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
            }

            zone {
                name = "zone"
                description = "test"

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            addDevicePathBSFailure()
            testImportImageFlagWhenAddBS()
        }
    }

    void testImportImageFlagWhenAddBS() {
        def imageUuid = "a603e80ea18f424f8a5f00371d484537"
        def bsUuid = Platform.getUuid()

        // same uuid occurs in metadata will be filtered out
        env.simulator(SftpBackupStorageConstant.GET_IMAGES_METADATA) {
            def rsp = new SftpBackupStorageCommands.GetImagesMetaDataRsp()
            rsp.imagesMetaData = "{\"uuid\":\"${imageUuid}\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}" +
                    "\n\n{\"uuid\":\"${imageUuid}\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}"
            return rsp
        }

        env.simulator(SftpBackupStorageConstant.CONNECT_PATH) {
            def rsp = new SftpBackupStorageCommands.ConnectResponse()
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1000)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
            return rsp
        }

        addSftpBackupStorage {
            name = "imagestore"
            description = "desc"
            username = "username"
            password = "password"
            hostname = "hostname"
            url = "/data"
            importImages = true
            resourceUuid = bsUuid
        }

        // same image uuid will be filter when restore image meta data
        // restored image will use a new uuid
        retryInSecs {
            assert Q.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.backupStorageUuid, bsUuid).count() == 1
            String nImageUuid = Q.New(ImageBackupStorageRefVO.class).select(ImageBackupStorageRefVO_.imageUuid).eq(ImageBackupStorageRefVO_.backupStorageUuid, bsUuid).findValue()

            if (nImageUuid == null) {
                assert false
            }

            assert !nImageUuid.equals(imageUuid)
        }
    }

    void addDevicePathBSFailure() {
        AddSftpBackupStorageAction action = new AddSftpBackupStorageAction()
        action.name = "sftp"
        action.url = "/dev/sftp"
        action.username = "root"
        action.password = "password"
        action.hostname = "192.168.0.3"
        action.sessionId = adminSession()
        AddSftpBackupStorageAction.Result res = action.call()
        assert res.error != null
        action.url = "/proc/xx"
        res = action.call()
        assert res.error != null
        action.url = "/sys/test"
        res = action.call()
        assert res.error != null
    }
}
