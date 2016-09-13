package org.zstack.header.volume;

public interface VolumeConstant {
    String SERVICE_ID = "volume";

    String ACTION_CATEGORY = "volume";

    String VOLUME_FORMAT_RAW = "raw";
    String VOLUME_FORMAT_QCOW2 = "qcow2";
    String VOLUME_FORMAT_VMTX = "vmtx";

    String QUOTA_DATA_VOLUME_NUM = "volume.data.num";
    String QUOTA_VOLUME_SIZE = "volume.capacity";

    enum Capability {
        MigrationInCurrentPrimaryStorage,
        MigrationToOtherPrimaryStorage
    }
}
