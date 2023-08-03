package org.zstack.header.volume;

public interface VolumeConstant {
    String SERVICE_ID = "volume";

    String ACTION_CATEGORY = "volume";

    String VOLUME_FORMAT_RAW = "raw";
    String VOLUME_FORMAT_QCOW2 = "qcow2";
    String VOLUME_FORMAT_VMTX = "vmtx";
    String VOLUME_FORMAT_VMDK = "vmdk";

    /**
     * disk is not a file. it is used for scene of VM Direct Disk
     */
    String VOLUME_FORMAT_DISK = "disk";
    /**
     * lun volume is exclude.
     */
    int DEFAULT_MAX_DATA_VOLUME_NUMBER = 24;

    enum Capability {
        MigrationInCurrentPrimaryStorage,
        MigrationToOtherPrimaryStorage
    }
}
