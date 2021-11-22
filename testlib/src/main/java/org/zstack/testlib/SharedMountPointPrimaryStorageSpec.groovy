package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/20.
 */
class SharedMountPointPrimaryStorageSpec extends PrimaryStorageSpec {
    SharedMountPointPrimaryStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec xspec) {
            def simulator = { arg1, arg2 ->
                xspec.simulator(arg1, arg2)
            }

            simulator(KvmBackend.CONNECT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.ConnectCmd.class)
                SharedMountPointPrimaryStorageSpec sspec = spec.specByUuid(cmd.uuid)
                assert sspec != null: "cannot find shared mount point storage[uuid:${cmd.uuid}]"

                def rsp = new KvmBackend.ConnectRsp()
                rsp.totalCapacity = sspec.totalCapacity
                rsp.availableCapacity = sspec.availableCapacity
                return rsp
            }

            simulator(KvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.CREATE_VOLUME_WITH_BACKING_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.DELETE_BITS_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH) {
                def rsp = new KvmBackend.CreateTemplateFromVolumeRsp()
                rsp.actualSize = 0
                rsp.size = 0
                return rsp
            }

            simulator(KvmBackend.UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH) { HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.RevertVolumeFromSnapshotCmd.class)
                def rsp = new KvmBackend.RevertVolumeFromSnapshotRsp()
                rsp.newVolumeInstallPath = cmd.snapshotInstallPath + "/newpath"
                return rsp
            }

            simulator(KvmBackend.REINIT_IMAGE_PATH) {
                def rsp = new KvmBackend.ReInitImageRsp()
                rsp.newVolumeInstallPath = "/new/path"
                return rsp
            }

            simulator(KvmBackend.MERGE_SNAPSHOT_PATH) {
                return new KvmBackend.MergeSnapshotRsp()
            }

            simulator(KvmBackend.GET_VOLUME_SIZE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.GetVolumeSizeCmd.class)
                KvmBackend.GetVolumeSizeRsp rsp = new KvmBackend.GetVolumeSizeRsp()
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

            simulator(KvmBackend.OFFLINE_MERGE_SNAPSHOT_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.CREATE_EMPTY_VOLUME_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.CREATE_FOLDER_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.CHECK_BITS_PATH){
                def rsp = new KvmBackend.CheckBitsRsp()
                rsp.existing = true
                return rsp
            }

            simulator(KvmBackend.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                def rsp = new KvmBackend.DownloadBitsFromKVMHostRsp()
                rsp.format = "qcow2"
                return rsp
            }

            simulator(KvmBackend.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                return new KvmBackend.AgentRsp()
            }

            simulator(KvmBackend.HARD_LINK_VOLUME) {
                return new KvmBackend.LinkVolumeNewDirRsp()
            }

            simulator(KvmBackend.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
                def rsp = new KvmBackend.GetDownloadBitsFromKVMHostProgressRsp()
                rsp.totalSize = 1L
                return rsp
            }

            simulator(KvmBackend.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.GetQcow2HashValueCmd.class)
                def rsp = new KvmBackend.GetQcow2HashValueRsp()
                rsp.hashValue = cmd.installPath
                return rsp
            }
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addSharedMountPointPrimaryStorage {
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
