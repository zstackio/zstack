package org.zstack.storage.snapshot;

import org.zstack.header.core.Completion;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/9/19 15:53
 */
public interface BeforeDeleteSnapshotExtensionPoint {
    void beforeDeleteSnapshot(List<VolumeSnapshotInventory> volumeSnapshotInventories, Completion completion);
}
