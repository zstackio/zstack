package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/3/23.
 */
public class PrimaryStorageCanonicalEvent {
    public static final String PRIMARY_STORAGE_DELETED_PATH = "/primaryStorage/delete";

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
}
