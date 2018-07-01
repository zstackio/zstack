package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

/**
 * Create by weiwang at 2018/6/14
 */
public interface AfterTakeLiveSnapshotsOnVolumes {
    void afterTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply, Completion completion);
}
