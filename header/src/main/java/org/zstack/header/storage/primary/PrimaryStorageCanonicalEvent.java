package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/3/23.
 */
public class PrimaryStorageCanonicalEvent {
    public static final String PRIMARY_STORAGE_DELETED_PATH = "/primaryStorage/delete";
    public static final String PRIMARY_STORAGE_STATUS_CHANGED_PATH = "/primaryStorage/status/change";
    public static final String PRIMARY_STORAGE_STATE_CHANGED_PATH = "/primaryStorage/state/change";
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

    @NeedJsonSchema
    public static class PrimaryStorageStateChangedData{
        private String primaryStorageUuid;
        private PrimaryStorageState oldState;
        private PrimaryStorageState newState;
        private PrimaryStorageInventory inventory;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public PrimaryStorageState getOldState() {
            return oldState;
        }

        public void setOldState(PrimaryStorageState oldState) {
            this.oldState = oldState;
        }

        public PrimaryStorageState getNewState() {
            return newState;
        }

        public void setNewState(PrimaryStorageState newState) {
            this.newState = newState;
        }

        public PrimaryStorageInventory getInventory() {
            return inventory;
        }

        public void setInventory(PrimaryStorageInventory inventory) {
            this.inventory = inventory;
        }
    }
}
