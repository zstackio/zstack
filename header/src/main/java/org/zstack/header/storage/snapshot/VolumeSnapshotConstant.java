package org.zstack.header.storage.snapshot;

/**
 */
public interface VolumeSnapshotConstant {
    String SERVICE_ID = "snapshot.volume";

    String ACTION_CATEGORY = "volumeSnapshot";

    VolumeSnapshotType HYPERVISOR_SNAPSHOT_TYPE = new VolumeSnapshotType("Hypervisor");
    VolumeSnapshotType STORAGE_SNAPSHOT_TYPE = new VolumeSnapshotType("Storage");

    String SNAPSHOT_MESSAGE_ROUTED = "SNAPSHOT_MESSAGE_ROUTED";

    String VOLUME_SNAPSHOT_STRUCT = "VolumeSnapshotStruct";
    String NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR = "needTakeSnapshotOnHypervisor";
    String NEED_BLOCK_STREAM_ON_HYPERVISOR = "needBlockStreamOnHypervisor";
}
