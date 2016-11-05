package org.zstack.header.storage.backup;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 12/9/16.
 */
public class AddBackupStorageStruct {
    private boolean importImages = false;
    private String type;
    private BackupStorageInventory backupStorageInventory;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isImportImages() {
        return importImages;
    }

    public void setImportImages(boolean importImages) {
        this.importImages = importImages;
    }



    public BackupStorageInventory getBackupStorageInventory() {
        return backupStorageInventory;
    }

    public void setBackupStorageInventory(BackupStorageInventory backupStorageInventory) {
        this.backupStorageInventory = backupStorageInventory;
    }
}
