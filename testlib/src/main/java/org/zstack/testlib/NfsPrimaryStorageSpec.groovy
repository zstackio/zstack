package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.primary.PingPrimaryStorageMsg
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.storage.primary.nfs.NfsPrimaryToSftpBackupKVMBackend
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by xing5 on 2017/2/13.
 */
class NfsPrimaryStorageSpec extends PrimaryStorageSpec {

    NfsPrimaryStorageSpec(EnvSpec envSpec) {
        super(envSpec)

        preCreate {
            envSpec.message(PingPrimaryStorageMsg.class) { PingPrimaryStorageMsg msg, CloudBus bus ->
                def reply = new MessageReply()
                bus.reply(msg, reply)
            }
        }

        postCreate {
            envSpec.revokeMessage(PingPrimaryStorageMsg.class, null)
        }
    }

    class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec xspec) {
            def simulator = { arg1, arg2 ->
                xspec.simulator(arg1, arg2)
            }

            simulator(NfsPrimaryStorageKVMBackend.GET_VOLUME_BASE_IMAGE_PATH) {
                def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
                rsp.path = "/some/fake/path"
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.UNMOUNT_PRIMARY_STORAGE_PATH) { HttpEntity<String> e ->
                Spec.checkHttpCallType(e, true)
                return new KVMAgentCommands.AgentResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.MOUNT_PRIMARY_STORAGE_PATH) { HttpEntity<String> e, EnvSpec espec ->
                Spec.checkHttpCallType(e, true)
                def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.MountCmd.class)
                NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
                def rsp = new NfsPrimaryStorageKVMBackendCommands.MountAgentResponse()
                rsp.totalCapacity = spec.totalCapacity
                rsp.availableCapacity = spec.availableCapacity
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.GET_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec espec ->
                def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.GetCapacityCmd.class)
                NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid)
                def rsp = new NfsPrimaryStorageKVMBackendCommands.GetCapacityResponse()
                rsp.totalCapacity = spec.totalCapacity
                rsp.availableCapacity = spec.availableCapacity
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.CREATE_EMPTY_VOLUME_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.CreateRootVolumeFromTemplateResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.CREATE_FOLDER_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            }

            simulator(NfsPrimaryToSftpBackupKVMBackend.DOWNLOAD_FROM_SFTP_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.DownloadBitsFromSftpBackupStorageResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.PING_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.DeleteResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.MOVE_BITS_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.MoveBitsRsp()
            }

            simulator(NfsPrimaryToSftpBackupKVMBackend.UPLOAD_TO_SFTP_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.UploadToSftpResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_MERGE) {
                return new NfsPrimaryStorageKVMBackendCommands.OfflineMergeSnapshotRsp()
            }

            simulator(NfsPrimaryStorageKVMBackend.CHECK_BITS_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.CheckIsBitsExistingRsp()
            }

            simulator(NfsPrimaryStorageKVMBackend.CREATE_EMPTY_VOLUME_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.CreateEmptyVolumeResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.CreateTemplateFromVolumeRsp()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.REINIT_IMAGE_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.ReInitImageRsp()
                rsp.newVolumeInstallPath = "/new/volume/install/path"
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotResponse()
                rsp.newVolumeInstallPath = "/new/volume/install/path"
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.REBASE_MERGE_SNAPSHOT_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.RebaseAndMergeSnapshotsResponse()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.CREATE_VOLUME_WITH_BACKING_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.CreateVolumeWithBackingRsp()
            }

            simulator(NfsPrimaryStorageKVMBackend.GET_VOLUME_SIZE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeCmd.class)
                NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeRsp rsp = new NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeRsp()
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

            simulator(NfsPrimaryStorageKVMBackend.MERGE_SNAPSHOT_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.MergeSnapshotResponse()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) { HttpEntity<String> e, EnvSpec espec ->
                def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
                NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
                def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
                rsp.totalCapacity = spec.totalCapacity
                rsp.availableCapacity = spec.availableCapacity
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.UPDATE_MOUNT_POINT_PATH) { HttpEntity<String> e, EnvSpec espec ->
                def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.UpdateMountPointCmd.class)
                NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
                def rsp = new NfsPrimaryStorageKVMBackendCommands.UpdateMountPointRsp()
                rsp.totalCapacity = spec.totalCapacity
                rsp.availableCapacity = spec.availableCapacity
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.CREATE_VOLUME_FROM_TEMPLATE_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.CreateRootVolumeFromTemplateResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.NFS_TO_NFS_MIGRATE_BITS_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.NfsToNfsMigrateBitsRsp()
            }

            simulator(NfsPrimaryStorageKVMBackend.NFS_REBASE_VOLUME_BACKING_FILE_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.NfsRebaseVolumeBackingFileRsp()
            }

            simulator(NfsPrimaryStorageKVMBackend.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.DownloadBitsFromKVMHostRsp()
            }

            simulator(NfsPrimaryStorageKVMBackend.HARD_LINK_VOLUME) {
                return new NfsPrimaryStorageKVMBackendCommands.LinkVolumeNewDirRsp()
            }

            simulator(NfsPrimaryStorageKVMBackend.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.GetDownloadBitsFromKVMHostProgressRsp()
                rsp.totalSize = 1L
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.GET_QCOW2_HASH_VALUE_PATH) { HttpEntity<String> e, EnvSpec espec ->
                def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.GetQcow2HashValueCmd.class)
                def rsp = new NfsPrimaryStorageKVMBackendCommands.GetQcow2HashValueRsp()
                rsp.hashValue = cmd.installPath
                return rsp
            }
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addNfsPrimaryStorage {
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
