package org.zstack.header.storage.backup;

/**
 */
public enum BackupStorageErrors {
    DOWNLOAD_ERROR(1000),
    ATTACH_ERROR(1001),
    DETACH_ERROR(1002),
    ALLOCATE_ERROR(1003);

    private String code;

    private BackupStorageErrors(int id) {
        code = String.format("BS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }

    public enum Opaque {
        RECONNECT_AGENT
    }
}
