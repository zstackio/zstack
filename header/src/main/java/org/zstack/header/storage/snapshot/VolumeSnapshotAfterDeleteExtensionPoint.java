package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

/**
 * Created by xing5 on 2016/5/3.
 */
public interface VolumeSnapshotAfterDeleteExtensionPoint {
    void volumeSnapshotAfterDeleteExtensionPoint(VolumeSnapshotInventory snapshot, Completion completion);
    void volumeSnapshotAfterFailedDeleteExtensionPoint(VolumeSnapshotInventory snapshot);
}
