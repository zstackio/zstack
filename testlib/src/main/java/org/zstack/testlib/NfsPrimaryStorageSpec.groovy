package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.primary.PingPrimaryStorageMsg
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
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
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.VFSFile
import org.zstack.testlib.vfs.Volume
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.path.PathUtil
import org.zstack.utils.path.PathUtils

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

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

    static VFS vfs(String uuid, EnvSpec spec) {
        return spec.getVirtualFileSystem("nfs-${uuid}")
    }

    static VFS vfs(NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentCommand cmd, EnvSpec spec) {
        return vfs(cmd.uuid, spec)
    }

    static class Simulators implements Simulator {
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

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.GET_VOLUME_BASE_IMAGE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.GetVolumeBaseImagePathCmd.class)
                VFS vfs = vfs(cmd, spec)
                List<String> backingFiles = []
                vfs.walkFileSystem { vfile ->
                    if (vfile.pathString().contains(cmd.volumeInstallDir)) {
                        Qcow2 qfile = vfile as Qcow2
                        if (qfile.backingFile != null && qfile.backingFile.toAbsolutePath().toString().startsWith(cmd.imageCacheDir)) {
                            backingFiles.add(qfile.backingFile.toAbsolutePath().toString())
                        }
                    }
                }

                assert backingFiles.size() == 1 : "zero or more than one backing file found. ${backingFiles}"
                rsp.path = backingFiles[0]
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
                return new NfsPrimaryStorageKVMBackendCommands.CreateEmptyVolumeResponse()
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.CREATE_EMPTY_VOLUME_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.CreateEmptyVolumeCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.createQcow2(cmd.installUrl, 0L, cmd.size)
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.CREATE_FOLDER_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.CREATE_FOLDER_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.CreateFolderCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.createDirectories(cmd.installUrl)
                return rsp
            }

            simulator(NfsPrimaryToSftpBackupKVMBackend.DOWNLOAD_FROM_SFTP_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.DownloadBitsFromSftpBackupStorageResponse()
            }

            VFS.vfsHook(NfsPrimaryToSftpBackupKVMBackend.DOWNLOAD_FROM_SFTP_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.DownloadBitsFromSftpBackupStorageCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.createQcow2(cmd.primaryStorageInstallPath, 0L)
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.PING_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.DeleteResponse()
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.DELETE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.DeleteCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.delete(cmd.installPath)
                return rsp
            }

//            simulator(NfsPrimaryStorageKVMBackend.LIST_PATH) {
//                return new NfsPrimaryStorageKVMBackendCommands.ListDirectionResponse()
//            }

            simulator(NfsPrimaryStorageKVMBackend.MOVE_BITS_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.MoveBitsRsp()
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.MOVE_BITS_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.MoveBitsCmd.class)
                VFS vfs = vfs(cmd, spec)
                VFSFile vfile = vfs.getFile(cmd.srcPath)
                vfile.move(cmd.destPath)
                return rsp
            }

            simulator(NfsPrimaryToSftpBackupKVMBackend.UPLOAD_TO_SFTP_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.UploadToSftpResponse()
            }

            simulator(NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_MERGE) {
                return new NfsPrimaryStorageKVMBackendCommands.OfflineMergeSnapshotRsp()
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_MERGE, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.OfflineMergeSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)

                Qcow2 src = vfs.getFile(cmd.srcPath)
                if (!cmd.fullRebase) {
                    src.rebase(cmd.destPath)
                } else {
                    // when full rebase requested
                    // general steps for offline Qcow2 merge operation has following steps:
                    // 1. create a temp Qcow2 file
                    // 2. convert Qcow2 on destPath to temp Qcow2
                    // 3. replace Qcow2 on destPath with temp file
                    if (!vfs.exists(cmd.destPath)) {
                        vfs.createQcow2(cmd.destPath, 0, 0)
                    }
                }

                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.CHECK_BITS_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.CheckIsBitsExistingRsp()
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.CHECK_BITS_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.CheckIsBitsExistingCmd.class)
                VFS vfs = vfs(cmd, spec)
                rsp.existing = vfs.exists(cmd.installPath)
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.CreateTemplateFromVolumeRsp()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.CREATE_TEMPLATE_FROM_VOLUME_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.CreateTemplateFromVolumeCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.getFile(cmd.rootVolumePath)
                vfs.createQcow2(cmd.installPath, 0L)
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.REINIT_IMAGE_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.ReInitImageRsp()
                rsp.newVolumeInstallPath = "/new/volume/install/path"
                return rsp
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.REINIT_IMAGE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.ReInitImageCmd.class)
                VFS vfs = vfs(cmd, xspec)
                Path volumePath = vfs.getPath(cmd.volumePath)
                String newVolumePath = volumePath.parent.resolve("${Platform.uuid}.qcow2").toString()
                vfs.createQcow2(newVolumePath, 0L, 0L, cmd.imagePath)
                rsp.newVolumeInstallPath = newVolumePath
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotResponse()
                rsp.newVolumeInstallPath = "/new/volume/install/path"
                return rsp
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)
                VFSFile sp = vfs.getFile(cmd.snapshotInstallPath)
                Path newVolumePath = sp.path.parent.resolve("${Platform.uuid}.qcow2")
                vfs.createQcow2(newVolumePath.toString(), 0L, 0L, sp.pathString())
                rsp.newVolumeInstallPath = newVolumePath.toString()
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
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.REBASE_MERGE_SNAPSHOT_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.RebaseAndMergeSnapshotsCmd.class)
                VFS vfs = vfs(cmd, spec)

                Qcow2 previous = null

                def rebase = { Qcow2 vfile ->
                    if (previous == null) {
                        previous = vfile
                        return
                    }

                    previous.rebase(vfile.pathString())
                    previous = vfile
                }

                cmd.snapshotInstallPaths.each {
                    Qcow2 sp = vfs.getFile(it)
                    rebase(sp)
                }

                Qcow2 latest = vfs.getFile(cmd.snapshotInstallPaths[0])
                vfs.createQcow2(cmd.workspaceInstallPath, 0L, 0L, latest.pathString())
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.GET_VOLUME_SIZE_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeRsp()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.GET_VOLUME_SIZE_PATH, xspec) { NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeCmd.class)
                VFS vfs = vfs(cmd, spec)
                Qcow2 file = vfs.getFile(cmd.installPath)
                rsp.size = file.virtualSize
                rsp.actualSize = file.actualSize
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.MERGE_SNAPSHOT_PATH) {
                def rsp = new NfsPrimaryStorageKVMBackendCommands.MergeSnapshotResponse()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.MERGE_SNAPSHOT_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.MergeSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.getFile(cmd.snapshotInstallPath)
                vfs.createQcow2(cmd.workspaceInstallPath, 0L)
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

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.CREATE_VOLUME_FROM_TEMPLATE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.CreateRootVolumeFromTemplateCmd.class)
                VFS vfs = vfs(cmd, xspec)
                Volume image = vfs.getFile(cmd.templatePathInCache, true)
                vfs.createQcow2(cmd.installUrl, image.actualSize, image.virtualSize, cmd.templatePathInCache)
                return rsp
            }

            simulator(NfsPrimaryStorageKVMBackend.NFS_TO_NFS_MIGRATE_BITS_PATH) {
                return new NfsPrimaryStorageKVMBackendCommands.NfsToNfsMigrateBitsRsp()
            }

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.NFS_TO_NFS_MIGRATE_BITS_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.NfsToNfsMigrateBitsCmd.class)
                VFS srcVfs = vfs(cmd, spec)

                PrimaryStorageVO dstNFS = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.url, cmd.url).find()
                assert dstNFS : "cannot find NFS primary storage[url: ${cmd.url}]"

                VFS dstVfs = vfs(dstNFS.uuid, spec)
                srcVfs.walkFileSystem { VFSFile f ->
                    if (f.pathString().contains(cmd.srcFolderPath)) {
                        String newPath = f.pathString().replace(cmd.srcFolderPath, cmd.dstFolderPath)
                        dstVfs.createFileFrom(Paths.get(newPath), f)
                    }
                }

                return rsp
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

            VFS.vfsHook(NfsPrimaryStorageKVMBackend.NFS_REBASE_VOLUME_BACKING_FILE_PATH, xspec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.NfsRebaseVolumeBackingFileCmd.class)
//                String dstPrimaryStorageUuid = getPrimaryStorageFromPath(cmd.dstPsMountPath)
//                VFS dstVfs = vfs(dstPrimaryStorageUuid, spec)
                //TODO: for guoyi
//                List<Qcow2> fileList = new ArrayList<>()
//                if (cmd.dstImageCacheTemplateFolderPath == null) {
//                    dstVfs.walkFileSystem { f ->
//                        if (f.pathString().contains(cmd.dstVolumeFolderPath)) {
//                            fileList.add(f)
//                        }
//                    }
//                } else {
//                    dstVfs.walkFileSystem { f ->
//                        if (f.pathString().contains(cmd.dstVolumeFolderPath)
//                                || f.pathString().contains(cmd.dstImageCacheTemplateFolderPath)) {
//                            fileList.add(f)
//                        }
//                    }
//                }
//
//                fileList.each { file ->
//                    if (file.backingFile == null) {
//                        return
//                    }
//
//                    file.backingFile = Paths.get(file.backingFile.toAbsolutePath().toString().replace(cmd.srcPsMountPath, cmd.dstPsMountPath))
//                    dstVfs.write(file.path, file.asJSONString())
//                }

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
