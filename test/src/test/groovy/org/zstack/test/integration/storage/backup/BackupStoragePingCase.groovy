package org.zstack.test.integration.storage.backup

import org.zstack.header.storage.backup.PingBackupStorageMsg
import org.zstack.sdk.BackupStorageInventory
import org.zstack.storage.backup.BackupStorageGlobalConfig
import org.zstack.storage.backup.BackupStoragePingTracker
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.TimeUnit

/**
 * Created by kayo on 2018/9/27.
 */
class BackupStoragePingCase extends SubCase {
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

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }
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
            BackupStorageGlobalConfig.PING_INTERVAL.updateValue(1)

            prepareEnv()
            testNoPingAfterBSDeleted()
            testPingAfterRescan()
        }
    }

    void prepareEnv() {
        env.simulator(SftpBackupStorageConstant.CONNECT_PATH) {
            def rsp = new SftpBackupStorageCommands.ConnectResponse()
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1000)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
            return rsp
        }
    }

    void testPingAfterRescan() {
        BackupStorageInventory bs = env.inventoryByName("sftp")
        BackupStoragePingTracker tracker = bean(BackupStoragePingTracker.class)
        tracker.reScanBackupStorage()

        int count = 0

        def cleanup = notifyWhenReceivedMessage(PingBackupStorageMsg.class) { PingBackupStorageMsg msg ->
            if (msg.backupStorageUuid == bs.uuid) {
                count ++
            }
        }

        retryInSecs {
            assert count > 0
        }

        cleanup()
    }

    void testNoPingAfterBSDeleted() {
        BackupStorageInventory bs = addSftpBackupStorage {
            name = "imagestore"
            description = "desc"
            username = "username"
            password = "password"
            hostname = "hostname"
            url = "/data"
            importImages = true
        }

        int count = 0
        def cleanup = notifyWhenReceivedMessage(PingBackupStorageMsg.class) { PingBackupStorageMsg msg ->
            if (msg.backupStorageUuid == bs.uuid) {
                count ++
            }
        }

        retryInSecs {
            assert count > 0
        }

        deleteBackupStorage { uuid = bs.uuid }

        count = 0

        TimeUnit.SECONDS.sleep(3L)

        assert count == 0

        cleanup()
    }
}
