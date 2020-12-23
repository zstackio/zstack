package org.zstack.sdk.storage.backup.s3;

import org.zstack.sdk.storage.backup.s3.S3BackupStorageInventory;

public class AddS3BackupStorageResult {
    public S3BackupStorageInventory inventory;
    public void setInventory(S3BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
    public S3BackupStorageInventory getInventory() {
        return this.inventory;
    }

}
