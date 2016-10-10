package org.zstack.header.storage.snapshot;

/**
 */
public interface VolumeSnapshotConstant {
    String SERVICE_ID = "snapshot.volume";

    String ACTION_CATEGORY = "volumeSnapshot";

    VolumeSnapshotType HYPERVISOR_SNAPSHOT_TYPE = new VolumeSnapshotType("Hypervisor");
    VolumeSnapshotType STORAGE_SNAPSHOT_TYPE = new VolumeSnapshotType("Storage");

    String SNAPSHOT_MESSAGE_ROUTED = "SNAPSHOT_MESSAGE_ROUTED";

    String QUOTA_VOLUME_SNAPSHOT_NUM = "snapshot.volume.num";
}
