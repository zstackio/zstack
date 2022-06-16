package org.zstack.header.storage.snapshot.group;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;

/**
 * Created by LiangHanYu on 2022/6/1 14:02
 */
public interface MemorySnapshotGroupExtensionPoint {
    void afterCreateMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, Completion completion);

    void beforeRevertMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, Completion completion);
}
