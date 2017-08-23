package org.zstack.header.storage.backup;

public interface BackupStorageConstant {
    public static final String SERVICE_ID = "storage.backup";

    public static final String SCHEME_NFS = "nfs";
    public static final String SCHEME_HTTP = "http";
    public static final String SCHEME_HTTPS = "https";
    public static final String SCHEME_FILE = "file";

    public static final String DEFAULT_ALLOCATOR_STRATEGY = "defaultAllocatorStrategy";
    public static final BackupStorageAllocatorStrategyType DEFAULT_ALLOCATOR_STRATEGY_TYPE = new BackupStorageAllocatorStrategyType(DEFAULT_ALLOCATOR_STRATEGY);

    public static enum AllocatorParams {
        SPEC,
        CANDIDATES,
    }

    public static final String ACTION_CATEGORY = "backupStorage";

    public static final String COMMIT_VOLUME_IMAGE_CREATE_SNAPSHOT_STAGE = "0-15";
    public static final String COMMIT_VOLUME_IMAGE_COMMIT_SNAPSHOT_STAGE = "15-30";
    public static final String COMMIT_VOLUME_IMAGE_UPLOAD_TEMPLATE_STAGE = "30-90";
    public static final String COMMIT_VOLUME_IMAGE_SYNC_SIZE_STAGE = "90-100";

    public static final String CREATE_ROOT_VOLUME_TEMPLATE_PREPARATION_STAGE = "0-10";
    public static final String CREATE_ROOT_VOLUME_TEMPLATE_CREATE_TEMPORARY_TEMPLATE_STAGE = "10-30";
    public static final String CREATE_ROOT_VOLUME_TEMPLATE_UPLOAD_STAGE = "30-90";
    public static final String CREATE_ROOT_VOLUME_TEMPLATE_SUBSEQUENT_EVENT_STAGE = "85-100";


}
