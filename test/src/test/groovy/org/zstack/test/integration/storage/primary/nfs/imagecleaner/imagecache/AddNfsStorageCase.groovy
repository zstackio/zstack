package org.zstack.test.integration.storage.primary.nfs.imagecleaner.imagecache

import org.zstack.sdk.AddLocalPrimaryStorageAction
import org.zstack.sdk.AddNfsPrimaryStorageAction
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec

/**
 * Created by camile on 2017/4/
 */
class AddNfsStorageCase extends SubCase {
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
            addErrorPathLSailure()
        }
    }

    void addErrorPathLSailure() {
        String zoneUuid = (env.specByName("zone") as ZoneSpec).inventory.uuid
        AddNfsPrimaryStorageAction addLocalPrimaryStorageAction = new AddNfsPrimaryStorageAction()
        addLocalPrimaryStorageAction.url = "/dev/test"
        addLocalPrimaryStorageAction.name = "test2"
        addLocalPrimaryStorageAction.zoneUuid = zoneUuid
        addLocalPrimaryStorageAction.sessionId = adminSession()
        AddNfsPrimaryStorageAction.Result res= addLocalPrimaryStorageAction.call()
        res.error !=null
        addLocalPrimaryStorageAction.url = "/proc/test"
        res= addLocalPrimaryStorageAction.call()
        res.error !=null
        addLocalPrimaryStorageAction.url = "/sys/test"
        res= addLocalPrimaryStorageAction.call()
        res.error !=null
    }
}
