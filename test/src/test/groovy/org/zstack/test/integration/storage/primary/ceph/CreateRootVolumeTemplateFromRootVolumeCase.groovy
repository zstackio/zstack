package org.zstack.test.integration.storage.primary.ceph

import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.image.ImageState
import org.zstack.header.image.ImageStatus
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant
import org.zstack.header.storage.snapshot.VolumeSnapshotState
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.volume.VolumeConstant
import org.zstack.header.volume.VolumeType
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by lining on 2017-9-10.
 */
class CreateRootVolumeTemplateFromRootVolumeCase extends SubCase{

    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            createImageFromRootVolumeWhenVmRunning()
        }
    }

    void createImageFromRootVolumeWhenVmRunning(){
        VmInstanceInventory vm = env.inventoryByName("test-vm")
        VolumeInventory rootVolume = vm.allVolumes.find { it.uuid == vm.rootVolumeUuid }

        assert 0 == Q.New(VolumeSnapshotVO.class).count()

        ImageInventory img = createRootVolumeTemplateFromRootVolume {
            name = "template"
            rootVolumeUuid = vm.getRootVolumeUuid()
            sessionId = loginAsAdmin().uuid
        }
        assert ImageState.Enabled.name() == img.state
        assert img.type == ImageConstant.ZSTACK_IMAGE_TYPE
        assert img.guestOsType == "CentOS"
        assert img.mediaType == ImageConstant.ImageMediaType.RootVolumeTemplate.name()
        assert img.platform == ImagePlatform.Linux.name()
        assert ImageStatus.Ready.name() == img.status
        assert null != img.uuid
        assert VolumeConstant.VOLUME_FORMAT_RAW == img.format


        VolumeSnapshotInventory snapshotInventory = queryVolumeSnapshot {
        }[0]
        assert snapshotInventory.primaryStorageUuid == rootVolume.primaryStorageUuid
        assert snapshotInventory.state == VolumeSnapshotState.Enabled.name()
        assert snapshotInventory.status == VolumeSnapshotStatus.Ready.name()
        assert null != snapshotInventory.treeUuid
        assert snapshotInventory.type == VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString()
        assert snapshotInventory.volumeUuid == vm.getRootVolumeUuid()
        assert snapshotInventory.volumeType == VolumeType.Root.name()
        assert null != snapshotInventory.uuid
        assert snapshotInventory.format == VolumeConstant.VOLUME_FORMAT_RAW

        createVmInstance {
            name = "newVm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = img.uuid
            l3NetworkUuids = [vm.getDefaultL3NetworkUuid()]
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
