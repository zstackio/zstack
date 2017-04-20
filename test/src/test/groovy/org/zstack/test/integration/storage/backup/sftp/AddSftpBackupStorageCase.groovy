package org.zstack.test.integration.storage.backup.sftp

import org.zstack.sdk.AddSftpBackupStorageAction
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

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
