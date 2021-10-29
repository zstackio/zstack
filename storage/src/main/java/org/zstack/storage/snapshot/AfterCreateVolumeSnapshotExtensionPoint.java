package org.zstack.storage.snapshot;

import org.zstack.header.core.Completion;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * @Author: DaoDao
 * @Date: 2021/11/8
 */
public interface AfterCreateVolumeSnapshotExtensionPoint {
    void afterCreateVolumeSnapshot(VolumeSnapshotInventory snapshot, Completion completion);
}
