package org.zstack.storage.snapshot;

import org.zstack.header.core.Completion;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/9/16 09:58
 */
public interface AfterRevertVolumeFromSnapshotExtensionPoint {
    void afterRevertVolumeFromSnapshot(VolumeSnapshotInventory inventory, Completion completion);
}
