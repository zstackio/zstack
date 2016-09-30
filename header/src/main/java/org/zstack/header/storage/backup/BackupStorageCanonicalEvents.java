package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/5/6.
 */
public class BackupStorageCanonicalEvents {
    public static final String BACKUP_STORAGE_STATUS_CHANGED = "/backupstorage/status/change";

    @NeedJsonSchema
    public static class BackupStorageStatusChangedData {
        private String backupStorageUuid;
        private String oldStatus;
        private String newStatus;
        private BackupStorageInventory inventory;

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public BackupStorageInventory getInventory() {
            return inventory;
        }

        public void setInventory(BackupStorageInventory inventory) {
            this.inventory = inventory;
        }
    }
}
