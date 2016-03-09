package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 */
public class CreateTemplateFromVolumeSnapshotReply extends MessageReply {
    public static class CreateTemplateFromVolumeSnapshotResult {
        private String backupStorageUuid;
        private String installPath;

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    private long size;
    private List<CreateTemplateFromVolumeSnapshotResult> results;

    public List<CreateTemplateFromVolumeSnapshotResult> getResults() {
        return results;
    }

    public void setResults(List<CreateTemplateFromVolumeSnapshotResult> results) {
        this.results = results;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
