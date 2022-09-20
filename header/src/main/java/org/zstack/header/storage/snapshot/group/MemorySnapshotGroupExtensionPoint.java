package org.zstack.header.storage.snapshot.group;

import org.zstack.header.core.Completion;

import java.util.List;

/**
 * Created by LiangHanYu on 2022/6/1 14:02
 */
public interface MemorySnapshotGroupExtensionPoint {
    void afterCreateMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, Completion completion);

    void beforeRevertMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, List<Object> bundles, Completion completion);

    String getArchiveBundleCanonicalName();

    Class getArchiveBundleClass();
}
