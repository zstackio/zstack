package org.zstack.storage.snapshot;


import org.zstack.header.storage.snapshot.VolumeSnapshotStruct;

import java.util.List;

/**
 * @Author: Lei Liu
 * @Date: 2024/04/28
 */
public interface BeforeCreateVolumeSnapshotExtensionPoint {
    void beforeCreateVolumeSnapshot(List<VolumeSnapshotStruct> volumesSnapshotsJobs);
}