package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

import java.util.List;

/**
 * Created by mingjian.deng on 2019/4/8.
 */
public interface VolumeSnapshotCheckExtensionPoint {
    void checkBeforeDeleteSnapshot(VolumeSnapshotInventory snapshot, Completion completion);
}
