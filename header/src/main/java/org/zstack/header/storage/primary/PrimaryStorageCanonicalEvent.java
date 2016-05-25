package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/3/23.
 */
public class PrimaryStorageCanonicalEvent {
    public static final String PRIMARY_STORAGE_DELETED_PATH = "/primaryStorage/delete";
    public static final String PRIMARY_STORAGE_STATUS_CHANGED_PATH = "/primaryStorage/status/change";

    @NeedJsonSchema
    public static class PrimaryStorageDeletedData {
        private String primaryStorageUuid;
        private PrimaryStorageInventory inventory;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public PrimaryStorageInventory getInventory() {
            return inventory;
        }

        public void setInventory(PrimaryStorageInventory inventory) {
            this.inventory = inventory;
        }
    }

    @NeedJsonSchema
    public static class PrimaryStorageStatusChangedData {
        private String primaryStorageUuid;
        private String oldStatus;
        private String newStatus;
        private PrimaryStorageInventory inventory;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
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

        public PrimaryStorageInventory getInventory() {
            return inventory;
        }

        public void setInventory(PrimaryStorageInventory inventory) {
            this.inventory = inventory;
        }
    }
}
