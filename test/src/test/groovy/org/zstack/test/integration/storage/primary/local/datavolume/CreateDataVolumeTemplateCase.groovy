package org.zstack.test.integration.storage.primary.local.datavolume

import org.zstack.core.db.Q
import org.zstack.header.image.ImageVO
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
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
    }
}
