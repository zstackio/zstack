package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

import java.util.Map;

/**
 * Create by weiwang at 2018/6/14
 */
public interface BeforeTakeLiveSnapshotsOnVolumes {
    void beforeTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg msg, Map flowData, Completion completion);
}
