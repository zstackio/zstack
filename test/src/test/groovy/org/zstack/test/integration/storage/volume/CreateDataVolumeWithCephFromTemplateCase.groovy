package org.zstack.test.integration.storage.volume

import org.zstack.core.db.Q
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction
import org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class CreateDataVolumeWithCephFromTemplateCase extends SubCase {
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
        env = VolumeEnv.cephStorageCephEnv()
        env.cephBackupStorage {
            name = "other-ceph-bs"
            fsid = "other-ceph-cluster"
            monUrls = ["root:password@127.0.0.4/?monPort=7777"]
        }
    }

    @Override
    void test() {
        env.create {
            attachBackupStorageToZone {
                backupStorageUuid = env.inventoryByName("other-ceph-bs").uuid
                zoneUuid = env.inventoryByName("zone").uuid
            }
            testCreateDataVolumeWithCephFromTemplate()
            testCreateDataVolumeWithCeph()
            testTemplateStorageCompatibility()
        }
    }

    void testTemplateStorageCompatibility() {
        PrimaryStorageInventory ceph = env.inventoryByName("ceph-ps")
        BackupStorageInventory bs = env.inventoryByName("ceph-bs")
        BackupStorageInventory otherBs = env.inventoryByName("other-ceph-bs")
        ImageInventory incompatibleImage = addImage {
            name = "different-cluster-template"
            url = "http://zstack.org/download/test-volume.qcow2"
            format = "qcow2"
            mediaType = "DataVolumeTemplate"
            backupStorageUuids = [otherBs.uuid]
        } as ImageInventory

        def restore = new CreateDataVolumeFromVolumeTemplateAction()
        restore.name = "different-cluster-volume"
        restore.imageUuid = incompatibleImage.uuid
        restore.primaryStorageUuid = ceph.uuid
        restore.sessionId = env.session.uuid
        def restoreResult = restore.call()
        assert restoreResult.error?.globalErrorCode == "ORG_ZSTACK_STORAGE_VOLUME_10088" :
                "Different FSID must be rejected during BS selection: actual=${JSONObjectUtil.toJsonString(restoreResult.error)}"

        ImageInventory replicatedImage = addImage {
            name = "replicated-data-template"
            url = "http://zstack.org/download/test-volume.qcow2"
            format = "qcow2"
            mediaType = "DataVolumeTemplate"
            backupStorageUuids = [otherBs.uuid, bs.uuid]
        } as ImageInventory
        assert replicatedImage.backupStorageRefs.size() == 2 :
                "Template must have both compatible and incompatible replicas: actual=${replicatedImage.backupStorageRefs}"

        VolumeInventory volume = createDataVolumeFromVolumeTemplate {
            name = "compatible-replica-volume"
            imageUuid = replicatedImage.uuid
            primaryStorageUuid = ceph.uuid
        } as VolumeInventory
        assert volume.primaryStorageUuid == ceph.uuid :
                "Compatible replica must remain usable: expected=${ceph.uuid}, actual=${volume.primaryStorageUuid}"

        def backup = new CreateDataVolumeTemplateFromVolumeAction()
        backup.name = "different-cluster-backup"
        backup.volumeUuid = volume.uuid
        backup.backupStorageUuids = [otherBs.uuid]
        backup.sessionId = env.session.uuid
        def backupResult = backup.call()
        assert backupResult.error?.details?.contains("ORG_ZSTACK_STORAGE_BACKUP_10013") :
                "Explicit BS with different FSID must fail allocation: actual=${JSONObjectUtil.toJsonString(backupResult.error)}"

        [[bs.uuid], null].each { targetBsUuids ->
            ImageInventory template = createDataVolumeTemplateFromVolume {
                name = "same-cluster-backup"
                volumeUuid = volume.uuid
                backupStorageUuids = targetBsUuids
            } as ImageInventory
            assert template.backupStorageRefs*.backupStorageUuid == [bs.uuid] :
                    "Explicit and automatic allocation must select same-cluster BS: expected=${bs.uuid}, actual=${template.backupStorageRefs}"
        }
    }

    void testCreateDataVolumeWithCeph() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        PrimaryStorageInventory ceph = env.inventoryByName("ceph-ps")
        VolumeInventory dataVolume = createDataVolume {
            name = 'test-volume-template-ceph-3'
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
            systemTags = ["capability::virtio-scsi", "ephemeral::shareable"]
        } as VolumeInventory

        assert Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.name, dataVolume.name).count() == 1
    }

    void testCreateDataVolumeWithCephFromTemplate() {
        HostInventory host = env.inventoryByName("kvm")
        PrimaryStorageInventory ceph = env.inventoryByName("ceph-ps")
        ImageInventory image = env.inventoryByName("image-data-volume")

        VolumeInventory dataVolume = createDataVolumeFromVolumeTemplate {
            name = 'test-volume-template-local-2'
            imageUuid = image.uuid
            primaryStorageUuid = ceph.uuid
            hostUuid = host.uuid
            systemTags = ["capability::virtio-scsi"]
        } as VolumeInventory

        assert Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.name, dataVolume.name).count() == 1
    }
}
