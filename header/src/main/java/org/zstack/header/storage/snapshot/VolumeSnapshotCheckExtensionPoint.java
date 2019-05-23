package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

/**
 * Created by mingjian.deng on 2019/4/8.
 */
public interface VolumeSnapshotCheckExtensionPoint {
    void checkBeforeDeleteSnapshot(VolumeSnapshotInventory snapshot, boolean volumeDelete, Completion completion);
}
