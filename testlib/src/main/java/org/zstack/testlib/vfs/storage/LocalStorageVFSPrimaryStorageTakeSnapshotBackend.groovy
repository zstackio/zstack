package org.zstack.testlib.vfs.storage

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.volume.VolumeInventory
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.primary.local.LocalStorageConstants
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.LocalStorageSpec
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.extensions.VFSPrimaryStorageTakeSnapshotBackend
import org.zstack.testlib.vfs.extensions.VFSSnapshot
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

class LocalStorageVFSPrimaryStorageTakeSnapshotBackend implements AbstractFileSystemBasedVFSPrimaryStorageBackend, VFSPrimaryStorageTakeSnapshotBackend {
    private static final CLogger logger = Utils.getLogger(LocalStorageVFSPrimaryStorageTakeSnapshotBackend.class)

    @Override
    String getPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE
    }

    @Override
    VFSSnapshot takeSnapshot(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.TakeSnapshotCmd cmd, VolumeInventory volume) {
        String storagePath = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.mountPath)
                .eq(PrimaryStorageVO_.uuid, volume.primaryStorageUuid)
                .findValue()

        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)
        String newVolumePath = cmd.newVolumeInstallPath != null ? cmd.newVolumeInstallPath :
                vfs.getPath(cmd.installPath).getParent().resolve("${Platform.uuid}.qcow2").toAbsolutePath().toString()

        Qcow2 previous = vfs.getFile(volume.installPath, true)

        VFSSnapshot snapshot = new VFSSnapshot()
        if (cmd.fullSnapshot && !cmd.isOnline()) {
            Qcow2 newBase = previous.copyWithoutBackingFile(cmd.installPath)
            vfs.createQcow2(newVolumePath, 0L, newBase.virtualSize, newBase.pathString())
            snapshot.installPath = newBase.pathString()
            snapshot.size = newBase.virtualSize
            cmd.setNewVolumeInstallPath(newVolumePath)
        } else if (cmd.fullSnapshot) {
            previous.rebase((String) null)
            vfs.createQcow2(cmd.installPath, previous.virtualSize, previous.virtualSize, previous.pathString())
            snapshot.installPath = previous.pathString()
            snapshot.size = previous.virtualSize
            cmd.setNewVolumeInstallPath(cmd.installPath)
        } else {
            vfs.createQcow2(cmd.installPath, previous.virtualSize, previous.virtualSize, previous.pathString())
            snapshot.installPath = previous.pathString()
            snapshot.size = previous.virtualSize
            cmd.setNewVolumeInstallPath(cmd.installPath)
        }

        return snapshot
    }

    @Override
    void mergeSnapshots(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.MergeSnapshotCmd cmd,
                        VolumeInventory volume) {
        VolumeSnapshotVO src = SQL.New("select sp from VolumeSnapshotVO sp, VolumeVO vol where sp.volumeUuid = vol.uuid and" +
                " vol.vmInstanceUuid = :vmUuid and sp.primaryStorageInstallPath = :srcPath" +
                " and vol.deviceId = :deviceId", VolumeSnapshotVO.class)
                .param("vmUuid", cmd.vmUuid)
                .param("srcPath", cmd.srcPath)
                .param("deviceId", cmd.volume.getDeviceId())
                .find()

        assert src : "cannot find source snapshot[path: ${cmd.srcPath}] of VM[uuid: ${cmd.vmUuid}] in database"
        VolumeSnapshotInventory sp = src.toInventory()

        String storagePath = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.mountPath)
                .eq(PrimaryStorageVO_.uuid, sp.primaryStorageUuid)
                .findValue()

        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)
        vfs.Assert(vfs.exists(sp.primaryStorageInstallPath), "missing snapshot install ${sp.primaryStorageInstallPath}")
        Qcow2 from = vfs.getFile(sp.primaryStorageInstallPath, true)
        vfs.Assert(vfs.exists(volume.installPath), "missing snapshot install ${sp.primaryStorageInstallPath}")
        Qcow2 to = vfs.getFile(volume.installPath, true)
        assert to.backingFile != null : "the target[${to.pathString()}] volume has no backing file"
        if (to.backingQcow2().pathString() == from.pathString()) {
            assert cmd.fullRebase : "the snapshot is volume's backing file, this must be a fullRebase: ${e.body}"
        }

        if (cmd.fullRebase) {
            to.rebase((String)null)
        } else {
            to.rebase(from.path)
        }
    }

    @Override
    List<TakeSnapshotsOnKvmResultStruct> takeSnapshotsOnVolumes(String primaryStorageUuid, HttpEntity<String> e, EnvSpec spec, List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {
        TakeSnapshotsOnKvmJobStruct job0 = snapshotJobs[0]
        String storagePath = SQL.New("select pri.mountPath from PrimaryStorageVO pri, VolumeVO vol where" +
                " pri.uuid = vol.primaryStorageUuid and vol.uuid = :uuid", String.class).param("uuid", job0.volumeUuid)
                .find()

        assert storagePath : "cannot find primary storage of snapshot[uuid: ${job0.snapshotUuid}]"

        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)
        doTakeSnapshotsOnVolumes(vfs, snapshotJobs)
    }

    @Override
    void blockStream(HttpEntity<String> e, EnvSpec spec, VolumeInventory volume) {
        String storagePath = SQL.New("select pri.mountPath from PrimaryStorageVO pri, VolumeVO vol where" +
                " pri.uuid = vol.primaryStorageUuid and vol.uuid = :uuid", String.class).param("uuid", volume.getUuid())
                .find()

        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)
        blockStream(vfs, volume)
    }
}
