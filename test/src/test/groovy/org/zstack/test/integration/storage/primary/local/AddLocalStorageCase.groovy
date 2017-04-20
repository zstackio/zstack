package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.AddLocalPrimaryStorageAction
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by camile on 2017/4/
 */
class AddLocalStorageCase extends SubCase {
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
        AddLocalPrimaryStorageAction addLocalPrimaryStorageAction = new AddLocalPrimaryStorageAction()
        addLocalPrimaryStorageAction.url = "/dev/test"
        addLocalPrimaryStorageAction.name = "test2"
        addLocalPrimaryStorageAction.zoneUuid = zoneUuid
        addLocalPrimaryStorageAction.sessionId = adminSession()
        AddLocalPrimaryStorageAction.Result res= addLocalPrimaryStorageAction.call()
        assert res.error !=null
        addLocalPrimaryStorageAction.url = "/proc/test"
        res= addLocalPrimaryStorageAction.call()
        assert res.error !=null
        addLocalPrimaryStorageAction.url = "/sys/test"
        res= addLocalPrimaryStorageAction.call()
        assert res.error !=null
    }
}
