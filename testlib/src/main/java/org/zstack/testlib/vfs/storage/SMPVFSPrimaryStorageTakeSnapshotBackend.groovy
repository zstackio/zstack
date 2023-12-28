package org.zstack.testlib.vfs.storage

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct
import org.zstack.header.volume.VolumeInventory
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.primary.smp.SMPConstants
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.NfsPrimaryStorageSpec
import org.zstack.testlib.SharedMountPointPrimaryStorageSpec
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.extensions.VFSPrimaryStorageTakeSnapshotBackend
import org.zstack.testlib.vfs.extensions.VFSSnapshot

class SMPVFSPrimaryStorageTakeSnapshotBackend implements AbstractFileSystemBasedVFSPrimaryStorageBackend, VFSPrimaryStorageTakeSnapshotBackend {
    @Override
    String getPrimaryStorageType() {
        return SMPConstants.SMP_TYPE
    }

    @Override
    VFSSnapshot takeSnapshot(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.TakeSnapshotCmd cmd, VolumeInventory volume) {
        def vfs = SharedMountPointPrimaryStorageSpec.vfs(volume.getPrimaryStorageUuid(), spec)
        if (cmd.isOnline()) {
            vfs.Assert(vfs.exists(cmd.installPath), "cannot find file[${cmd.installPath}]")
            vfs.delete(cmd.installPath)
        }
        return doTakeSnapshot(vfs, cmd, volume)
    }

    @Override
    void mergeSnapshots(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.MergeSnapshotCmd cmd, VolumeInventory volume) {

    }

    @Override
    List<TakeSnapshotsOnKvmResultStruct> takeSnapshotsOnVolumes(String primaryStorageUuid, HttpEntity<String> e, EnvSpec spec, List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {
        return doTakeSnapshotsOnVolumes(SharedMountPointPrimaryStorageSpec.vfs(primaryStorageUuid, spec), snapshotJobs)
    }

    @Override
    void blockStream(HttpEntity<String> e, EnvSpec spec, VolumeInventory volume) {
        blockStream(SharedMountPointPrimaryStorageSpec.vfs(volume.getPrimaryStorageUuid(), spec), volume)
    }
}
