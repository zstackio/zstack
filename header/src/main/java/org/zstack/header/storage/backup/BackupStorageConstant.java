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
}
