package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.core.config.GlobalConfigVO
import org.zstack.core.config.GlobalConfigVO_
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStoragePrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*
import org.zstack.utils.gson.JSONObjectUtil


class LocalStorageCreateVolumeByPreallocationCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnvForPrimaryStorage()
    }

    @Override
    void test() {
        env.create {
            testLocalStorageCreateVolumeByPreallocation()
        }
    }

    void testLocalStorageCreateVolumeByPreallocation() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        HostInventory host = env.inventoryByName("kvm")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("test-vm")

        LocalStorageKvmBackend.CreateEmptyVolumeCmd cmd = null
        env.afterSimulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) {
            LocalStorageKvmBackend.CreateEmptyVolumeRsp rsp, HttpEntity<String> e ->
                cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CreateEmptyVolumeCmd.class)
                rsp.success = true
                return rsp
        }

        assert Q.New(GlobalConfigVO.class).eq(GlobalConfigVO_.name, "qcow2.allocation").eq(GlobalConfigVO_.category, "localStoragePrimaryStorage").select(GlobalConfigVO_.value).findValue() == "none"
        createDataVolume {
            name = "v1"
            diskOfferingUuid = diskOfferingInventory.uuid
            primaryStorageUuid = primaryStorageSpec.inventory.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }
        retryInSecs {
            assert cmd.preallocation == ""
        }

        updateGlobalConfig {
            category = LocalStoragePrimaryStorageGlobalConfig.CATEGORY
            name = LocalStoragePrimaryStorageGlobalConfig.QCOW2_ALLOCATION.name
            value = "metadata"
        }
        assert Q.New(GlobalConfigVO.class).eq(GlobalConfigVO_.name, "qcow2.allocation").eq(GlobalConfigVO_.category, "localStoragePrimaryStorage").select(GlobalConfigVO_.value).findValue() == "metadata"
        createDataVolume {
            name = "v2"
            diskOfferingUuid = diskOfferingInventory.uuid
            primaryStorageUuid = primaryStorageSpec.inventory.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }
        retryInSecs {
            assert cmd.preallocation == " -o preallocation=metadata "
        }

        updateGlobalConfig {
            category = LocalStoragePrimaryStorageGlobalConfig.CATEGORY
            name = LocalStoragePrimaryStorageGlobalConfig.QCOW2_ALLOCATION.name
            value = "full"
        }
        assert Q.New(GlobalConfigVO.class).eq(GlobalConfigVO_.name, "qcow2.allocation").eq(GlobalConfigVO_.category, "localStoragePrimaryStorage").select(GlobalConfigVO_.value).findValue() == "full"
        createDataVolume {
            name = "v3"
            diskOfferingUuid = diskOfferingInventory.uuid
            primaryStorageUuid = primaryStorageSpec.inventory.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }
        retryInSecs {
            assert cmd.preallocation == " -o preallocation=full "
        }

        updateGlobalConfig {
            category = LocalStoragePrimaryStorageGlobalConfig.CATEGORY
            name = LocalStoragePrimaryStorageGlobalConfig.QCOW2_ALLOCATION.name
            value = "falloc"
        }
        assert Q.New(GlobalConfigVO.class).eq(GlobalConfigVO_.name, "qcow2.allocation").eq(GlobalConfigVO_.category, "localStoragePrimaryStorage").select(GlobalConfigVO_.value).findValue() == "falloc"
        createDataVolume {
            name = "v4"
            diskOfferingUuid = diskOfferingInventory.uuid
            primaryStorageUuid = primaryStorageSpec.inventory.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }
        retryInSecs {
            assert cmd.preallocation == " -o preallocation=falloc "
        }

        updateGlobalConfig {
            category = LocalStoragePrimaryStorageGlobalConfig.CATEGORY
            name = LocalStoragePrimaryStorageGlobalConfig.QCOW2_ALLOCATION.name
            value = "none"
        }
        assert Q.New(GlobalConfigVO.class).eq(GlobalConfigVO_.name, "qcow2.allocation").eq(GlobalConfigVO_.category, "localStoragePrimaryStorage").select(GlobalConfigVO_.value).findValue() == "none"
        expectError {
            updateGlobalConfig {
                category = LocalStoragePrimaryStorageGlobalConfig.CATEGORY
                name = LocalStoragePrimaryStorageGlobalConfig.QCOW2_ALLOCATION.name
                value = "error"
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
