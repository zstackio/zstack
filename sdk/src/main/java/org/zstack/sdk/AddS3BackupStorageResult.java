package org.zstack.sdk;

import org.zstack.sdk.S3BackupStorageInventory;

public class AddS3BackupStorageResult {
    public S3BackupStorageInventory inventory;
    public void setInventory(S3BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
    public S3BackupStorageInventory getInventory() {
        return this.inventory;
    }

}
