package org.zstack.header.storage.snapshot;

/**
 */
public interface VolumeSnapshotConstant {
    public static final String SERVICE_ID = "snapshot.volume";

    public static final String ACTION_CATEGORY = "volumeSnapshot";

    public static final VolumeSnapshotType HYPERVISOR_SNAPSHOT_TYPE = new VolumeSnapshotType("Hypervisor");
    public static final VolumeSnapshotType STORAGE_SNAPSHOT_TYPE = new VolumeSnapshotType("Storage");

    public static final String SNAPSHOT_MESSAGE_ROUTED = "SNAPSHOT_MESSAGE_ROUTED";
}
