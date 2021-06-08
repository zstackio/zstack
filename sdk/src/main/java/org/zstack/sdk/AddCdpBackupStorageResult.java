package org.zstack.sdk;

import org.zstack.sdk.CdpBackupStorageInventory;

public class AddCdpBackupStorageResult {
    public CdpBackupStorageInventory inventory;
    public void setInventory(CdpBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
    public CdpBackupStorageInventory getInventory() {
        return this.inventory;
    }

}
