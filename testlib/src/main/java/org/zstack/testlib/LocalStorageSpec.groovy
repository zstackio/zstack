package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/20.
 */
class LocalStorageSpec extends PrimaryStorageSpec {

    LocalStorageSpec(EnvSpec envSpec) {
        super(envSpec)

        preCreate {
            setupSimulator()
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addLocalPrimaryStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
        } as PrimaryStorageInventory

        postCreate {
            inventory = queryPrimaryStorage {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    private void setupSimulator() {
        simulator(LocalStorageKvmBackend.GET_QCOW2_REFERENCE) {
            return new LocalStorageKvmBackend.GetQCOW2ReferenceRsp()
        }

        simulator(LocalStorageKvmBackend.GET_BASE_IMAGE_PATH) {
            def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
            rsp.path = "/some/patch"
            return rsp
        }

        simulator(LocalStorageKvmBackend.GET_BACKING_FILE_PATH) {
            def rsp = new LocalStorageKvmBackend.GetBackingFileRsp()
            rsp.backingFilePath = "/some/path"
            rsp.size = 0
            return rsp
        }

        simulator(LocalStorageKvmBackend.GET_MD5_PATH) {
            def rsp = new LocalStorageKvmBackend.GetMd5Rsp()
            rsp.md5s = []
            return rsp
        }

        simulator(LocalStorageKvmBackend.CHECK_MD5_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        simulator(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        simulator(LocalStorageKvmMigrateVmFlow.REBASE_ROOT_VOLUME_TO_BACKING_FILE_PATH) {
            return new LocalStorageKvmBackend.RebaseRootVolumeToBackingFileRsp()
        }

        simulator(LocalStorageKvmMigrateVmFlow.REBASE_SNAPSHOT_BACKING_FILES_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        simulator(LocalStorageKvmMigrateVmFlow.VERIFY_SNAPSHOT_CHAIN_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.InitCmd.class)
            LocalStorageSpec lspec = spec.specByUuid(cmd.uuid)
            assert lspec != null: "cannot find local storage[uuid:${cmd.uuid}]"

            def rsp = new LocalStorageKvmBackend.AgentResponse()
            rsp.totalCapacity = lspec.totalCapacity
            rsp.availableCapacity = lspec.availableCapacity
            return rsp
        }

        simulator(LocalStorageKvmBackend.CHECK_BITS_PATH) {
            def rsp = new LocalStorageKvmBackend.CheckBitsRsp()
            rsp.existing = true
            return rsp
        }

        simulator(LocalStorageKvmBackend.GET_PHYSICAL_CAPACITY_PATH) {
            return new LocalStorageKvmBackend.AgentResponse()
        }

        simulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) {
            return new LocalStorageKvmBackend.CreateEmptyVolumeRsp()
        }

        simulator(LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) {
            return new LocalStorageKvmBackend.CreateVolumeFromCacheRsp()
        }

        simulator(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            return new LocalStorageKvmBackend.DeleteBitsRsp()
        }

        simulator(LocalStorageKvmBackend.DELETE_DIR_PATH) {
            return new LocalStorageKvmBackend.DeleteBitsRsp()
        }

        simulator(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) {
            return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsRsp()
        }

        simulator(LocalStorageKvmSftpBackupStorageMediatorImpl.UPLOAD_BIT_PATH) {
            return new LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsRsp()
        }

        simulator(LocalStorageKvmBackend.CREATE_TEMPLATE_FROM_VOLUME) {
            return new LocalStorageKvmBackend.CreateTemplateFromVolumeRsp()
        }

        simulator(LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH) {
            return new LocalStorageKvmBackend.RevertVolumeFromSnapshotRsp()
        }

        simulator(LocalStorageKvmBackend.MERGE_AND_REBASE_SNAPSHOT_PATH) {
            return new LocalStorageKvmBackend.RebaseAndMergeSnapshotsRsp()
        }

        simulator(LocalStorageKvmBackend.MERGE_SNAPSHOT_PATH) {
            return new LocalStorageKvmBackend.MergeSnapshotRsp()
        }

        simulator(LocalStorageKvmBackend.GET_VOLUME_SIZE) {
            return new LocalStorageKvmBackend.GetVolumeSizeRsp()
        }

        simulator(LocalStorageKvmBackend.OFFLINE_MERGE_PATH) {
            return new LocalStorageKvmBackend.OfflineMergeSnapshotRsp()
        }
    }
}
