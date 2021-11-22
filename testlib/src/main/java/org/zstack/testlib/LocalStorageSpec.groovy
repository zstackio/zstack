package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
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
    }

    static class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }
            
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

            simulator(LocalStorageKvmBackend.GET_MD5_PATH) {HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetMd5Cmd.class)
                def rsp = new LocalStorageKvmBackend.GetMd5Rsp()
                rsp.md5s = []
                cmd.md5s.forEach{it ->
                    def t = new LocalStorageKvmBackend.Md5TO()
                    t.resourceUuid = it.resourceUuid
                    t.path = it .path
                    t.md5 = "mockmd5" + it.resourceUuid.substring(7)
                    rsp.md5s.add(t)
                }
                return rsp
            }

            simulator(LocalStorageKvmBackend.CHECK_MD5_PATH) {
                return new LocalStorageKvmBackend.AgentResponse()
            }

            simulator(LocalStorageKvmBackend.GET_BASE_IMAGE_PATH) {
                def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
                rsp.path = "/some/patch"
                rsp.size = 0
                return rsp
            }

            simulator(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) { HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd.class)
                def rsp = new LocalStorageKvmBackend.AgentResponse()
                if (cmd.volumeUuid == null || cmd.uuid == cmd.volumeUuid) {
                    rsp.success = false
                }
                return rsp
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

                def rsp = new LocalStorageKvmBackend.InitRsp()
                rsp.localStorageUsedCapacity = 0L

                LocalStorageSpec lspec = spec.specByUuid(cmd.uuid)
                if (lspec != null) {
                    rsp.totalCapacity = lspec.totalCapacity
                    rsp.availableCapacity = lspec.availableCapacity
                }

                return rsp
            }

            simulator(LocalStorageKvmBackend.CHECK_BITS_PATH) {
                def rsp = new LocalStorageKvmBackend.CheckBitsRsp()
                rsp.existing = true
                return rsp
            }

            simulator(LocalStorageKvmBackend.GET_PHYSICAL_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetPhysicalCapacityCmd.class)
                LocalStorageSpec lspec = spec.specByUuid(cmd.uuid)
                assert lspec != null: "cannot find local storage[uuid:${cmd.uuid}]"

                def rsp = new LocalStorageKvmBackend.AgentResponse()
                rsp.totalCapacity = lspec.totalCapacity
                rsp.availableCapacity = lspec.availableCapacity
                return rsp
            }

            simulator(LocalStorageKvmBackend.CREATE_VOLUME_WITH_BACKING_PATH) {
                return new LocalStorageKvmBackend.CreateVolumeWithBackingRsp()
            }

            simulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) {
                return new LocalStorageKvmBackend.CreateEmptyVolumeRsp()
            }

            simulator(LocalStorageKvmBackend.CREATE_FOLDER_PATH) {
                return new LocalStorageKvmBackend.AgentResponse()
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

            simulator(LocalStorageKvmBackend.REINIT_IMAGE_PATH) {
                def rsp = new LocalStorageKvmBackend.ReinitImageRsp()
                rsp.newVolumeInstallPath = "/new/snapshot/install/path"
                return rsp
            }

            simulator(LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH) { HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.RevertVolumeFromSnapshotCmd.class)
                def rsp = new LocalStorageKvmBackend.RevertVolumeFromSnapshotRsp()
                rsp.newVolumeInstallPath = cmd.snapshotInstallPath + "/${Platform.uuid}".toString()
                return rsp
            }

            simulator(LocalStorageKvmBackend.MERGE_AND_REBASE_SNAPSHOT_PATH) {
                return new LocalStorageKvmBackend.RebaseAndMergeSnapshotsRsp()
            }

            simulator(LocalStorageKvmBackend.MERGE_SNAPSHOT_PATH) {
                return new LocalStorageKvmBackend.MergeSnapshotRsp()
            }

            simulator(LocalStorageKvmBackend.GET_VOLUME_SIZE) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetVolumeSizeCmd.class)
                LocalStorageKvmBackend.GetVolumeSizeRsp rsp = new LocalStorageKvmBackend.GetVolumeSizeRsp()
                Long size = Q.New(VolumeVO.class).select(VolumeVO_.size).eq(VolumeVO_.uuid, cmd.volumeUuid).findValue()
                boolean isSnapshotExist = Q.New(VolumeSnapshotVO.class)
                        .eq(VolumeSnapshotVO_.volumeUuid, cmd.volumeUuid)
                        .exists
                if (!isSnapshotExist) {
                    rsp.actualSize = Q.New(VolumeVO.class).select(VolumeVO_.actualSize).eq(VolumeVO_.uuid, cmd.volumeUuid).findValue()
                } else {
                    rsp.actualSize = 1L
                }
                rsp.size = size
                return rsp
            }

            simulator(LocalStorageKvmBackend.OFFLINE_MERGE_PATH) {
                return new LocalStorageKvmBackend.OfflineMergeSnapshotRsp()
            }

            simulator(LocalStorageKvmBackend.CHECK_INITIALIZED_FILE) {
                return new LocalStorageKvmBackend.CheckInitializedFileRsp()
            }

            simulator(LocalStorageKvmBackend.CREATE_INITIALIZED_FILE) {
                return new LocalStorageKvmBackend.AgentResponse()
            }

            simulator(LocalStorageKvmBackend.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                def rsp = new LocalStorageKvmBackend.DownloadBitsFromKVMHostRsp()
                rsp.format = "qcow2"
                return new LocalStorageKvmBackend.AgentResponse()
            }

            simulator(LocalStorageKvmBackend.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                return new LocalStorageKvmBackend.AgentResponse()
            }


            simulator(LocalStorageKvmBackend.HARD_LINK_VOLUME) {
                return new LocalStorageKvmBackend.LinkVolumeNewDirRsp()
            }

            simulator(LocalStorageKvmBackend.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
                LocalStorageKvmBackend.GetDownloadBitsFromKVMHostProgressRsp rsp = new LocalStorageKvmBackend.GetDownloadBitsFromKVMHostProgressRsp()
                rsp.totalSize = 1L
                return rsp
            }

            simulator(LocalStorageKvmBackend.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.GetQcow2HashValueCmd.class)
                LocalStorageKvmBackend.GetQcow2HashValueRsp rsp = new LocalStorageKvmBackend.GetQcow2HashValueRsp()
                rsp.hashValue = cmd.installPath
                return rsp
            }
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
}
