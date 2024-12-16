package org.zstack.abstraction.storage;

import org.zstack.abstraction.PluginDriver;

import java.util.Map;

// storage sdk should support
// volume creation
// volume update
// volume deletion
// volume resize
// volume migration
// volume attach
// volume detach
// volume snapshot creation
// volume snapshot deletion
// volume snapshot update
// volume snapshot rollback
// volume snapshot backup
// volume backup creation
// volume backup deletion
// volume backup update
// volume backup restore
// volume backup sync to remote
// volume backup sync from remote to local
// ping should be supported
public interface StorageDriver extends PluginDriver {
    boolean initialize(Map<String, String> properties);

    boolean connect(Map<String, String> properties);

    boolean ping(Map<String, String> properties);

    boolean createVolume(VolumeData volume);

    boolean deleteVolume(VolumeData volume);

    boolean resizeVolume(VolumeData volume);

    boolean migrateVolume(VolumeData volume);

    boolean createVolumeSnapshot(VolumeSnapshotData volumeSnapshot);

    boolean deleteVolumeSnapshot(VolumeSnapshotData volumeSnapshot);

    boolean updateVolumeSnapshot(VolumeSnapshotData volumeSnapshot);

    boolean rollbackVolumeSnapshot(VolumeSnapshotData volumeSnapshot);

    boolean backupVolumeSnapshot(VolumeSnapshotData volumeSnapshot);

    boolean createVolumeBackup(VolumeBackupData volumeBackup);

    boolean deleteVolumeBackup(VolumeBackupData volumeBackup);

    boolean updateVolumeBackup(VolumeBackupData volumeBackup);

    boolean restoreVolumeBackup(VolumeBackupData volumeBackup);

    boolean syncVolumeBackupToRemote(VolumeBackupData volumeBackup);

    boolean syncVolumeBackupFromRemoteToLocal(VolumeBackupData volumeBackup);
}
