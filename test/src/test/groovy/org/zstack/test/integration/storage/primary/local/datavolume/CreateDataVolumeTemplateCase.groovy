package org.zstack.test.integration.storage.primary.local.datavolume

import org.zstack.core.db.Q
import org.zstack.header.image.ImageVO
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.KVMHostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
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
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        DiskOfferingSpec disk = env.specByName("diskOffering")
        BackupStorageSpec bs = env.specByName("sftp")
        KVMHostSpec kvm = env.specByName("kvm")
        VmSpec vm = env.specByName("test-vm")

        def count = Q.New(ImageVO.class).count()

        VolumeInventory dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.inventory.uuid
            primaryStorageUuid = primaryStorageSpec.inventory.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.inventory.uuid)
        }

        ImageInventory image = createDataVolumeTemplateFromVolume {
            name = "data-volume"
            volumeUuid = dataVolume.uuid
            backupStorageUuids = [bs.inventory.uuid]
        }

        assert image.name == "data-volume"

        attachDataVolumeToVm {
            volumeUuid = dataVolume.uuid
            vmInstanceUuid = vm.inventory.uuid
        }

        image = createDataVolumeTemplateFromVolume {
            name = "data-volume-1"
            volumeUuid = dataVolume.uuid
            backupStorageUuids = [bs.inventory.uuid]
        }

        assert image.name == "data-volume-1"
        assert Q.New(ImageVO.class).count() == count + 2
    }
}
