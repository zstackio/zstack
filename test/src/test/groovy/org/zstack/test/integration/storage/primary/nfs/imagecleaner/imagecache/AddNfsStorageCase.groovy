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
        AddNfsPrimaryStorageAction addNfsPrimaryStorageAction = new AddNfsPrimaryStorageAction()
        addNfsPrimaryStorageAction.url = "192.168.1.196:/dev/test"
        addNfsPrimaryStorageAction.name = "test2"
        addNfsPrimaryStorageAction.zoneUuid = zoneUuid
        addNfsPrimaryStorageAction.sessionId = adminSession()
        AddNfsPrimaryStorageAction.Result res= addNfsPrimaryStorageAction.call()
        assert res.error !=null
        addNfsPrimaryStorageAction.url = "192.168.1.196:/proc/test"
        res= addNfsPrimaryStorageAction.call()
        assert res.error !=null
        addNfsPrimaryStorageAction.url = "192.168.1.196:/sys/test"
        res= addNfsPrimaryStorageAction.call()
        assert res.error !=null
    }
}
