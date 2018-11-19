package org.zstack.test.integration.storage.primary.local.datavolume

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.image.ImageVO
import org.zstack.image.ImageQuotaConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by mingjian.deng on 2017/10/19.
 */
class CreateDataVolumeTemplateCase extends SubCase {
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
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            testCreateDataVolumeTemplate()
            testCreateDataVolumeTemplateQuota()
        }
    }

    /**
     * 1. create account
     * 2. create vm use the account
     * 3. create data volume use the account
     * 4. update image size quota of the account to 0
     * 5. create template of root or data volume will fail
     * 6. recover image size quota of the account
     * and set image number quota as the used image of the account
     * 7. create template of root or data volume will fail
     */
    void testCreateDataVolumeTemplateQuota() {
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory
        def vm = env.inventoryByName("test-vm") as VmInstanceInventory
        def image = env.inventoryByName("test-iso") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def l3 = env.inventoryByName("pubL3") as L3NetworkInventory

        shareResource {
            resourceUuids = [offer.uuid, disk.uuid, image.uuid]
            toPublic = true
        }

        def account = createAccount {
            name = "test"
            password = "test"
        } as AccountInventory

        def session = logInByAccount {
            accountName = "test"
            password = "test"
        } as SessionInventory

        def vm1 = createVmInstance {
            name = "vm1"
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            rootDiskOfferingUuid = disk.uuid
            sessionId = session.uuid
        } as VmInstanceInventory

        def dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = ps.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
            sessionId = session.uuid
        } as VolumeInventory

        updateQuota {
            identityUuid = account.uuid
            name = ImageQuotaConstant.IMAGE_SIZE
            value = 0
        }

        expect(AssertionError.class) {
            createDataVolumeTemplateFromVolume {
                name = "data-volume"
                volumeUuid = dataVolume.uuid
                backupStorageUuids = [bs.uuid]
                sessionId = session.uuid
            } as ImageInventory
        }

        expect(AssertionError.class) {
            createRootVolumeTemplateFromRootVolume {
                name = "data-volume"
                rootVolumeUuid = vm1.getRootVolumeUuid()
                backupStorageUuids = [bs.uuid]
                sessionId = session.uuid
            } as ImageInventory
        }

        updateQuota {
            identityUuid = account.uuid
            name = ImageQuotaConstant.IMAGE_SIZE
            value = SizeUnit.GIGABYTE.toByte(1000)
        }

        createDataVolumeTemplateFromVolume {
            name = "root-volume"
            volumeUuid = dataVolume.uuid
            backupStorageUuids = [bs.uuid]
            sessionId = session.uuid
        } as ImageInventory

        def count = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceType, ImageVO.class.getSimpleName())
                .eq(AccountResourceRefVO_.accountUuid, account.uuid)
                .count()

        updateQuota {
            identityUuid = account.uuid
            name = ImageQuotaConstant.IMAGE_NUM
            value = count
        }

        expect(AssertionError.class) {
            createDataVolumeTemplateFromVolume {
                name = "data-volume"
                volumeUuid = dataVolume.uuid
                backupStorageUuids = [bs.uuid]
                sessionId = session.uuid
            } as ImageInventory
        }

        expect(AssertionError.class) {
            createRootVolumeTemplateFromRootVolume {
                name = "root-volume"
                rootVolumeUuid = vm1.getRootVolumeUuid()
                backupStorageUuids = [bs.uuid]
                sessionId = session.uuid
            } as ImageInventory
        }
    }

    void testCreateDataVolumeTemplate() {
        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory
        def vm = env.inventoryByName("test-vm") as VmInstanceInventory

        def count = Q.New(ImageVO.class).count()

        def dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = ps.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
        } as VolumeInventory

        def image = createDataVolumeTemplateFromVolume {
            name = "data-volume"
            volumeUuid = dataVolume.uuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        assert image.name == "data-volume"

        attachDataVolumeToVm {
            volumeUuid = dataVolume.uuid
            vmInstanceUuid = vm.uuid
        }

        image = createDataVolumeTemplateFromVolume {
            name = "data-volume-1"
            volumeUuid = dataVolume.uuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        assert image.name == "data-volume-1"
        assert Q.New(ImageVO.class).count() == count + 2

        // make sure create data volume is copy from image
        LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd cmd
        env.simulator(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) {HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.getBody(), LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd.class)
            return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsRsp()
        }

        def vol = createDataVolumeFromVolumeTemplate {
            primaryStorageUuid = ps.uuid
            imageUuid = image.uuid
            hostUuid = kvm.uuid
            name = "test"
        } as VolumeInventory

        assert cmd != null
        assert vol.installPath == cmd.primaryStorageInstallPath :
                "you change the create data volume from template logic, make sure the imageCacheVO will be created correctly after volume migrated!!!"
    }
}
