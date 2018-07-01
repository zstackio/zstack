package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

/**
 * Create by weiwang at 2018/6/14
 */
public interface AfterTakeLiveSnapshotsOnVolumesFailed {
    void afterTakeLiveSnapshotsOnVolumesFailed(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply);
}
