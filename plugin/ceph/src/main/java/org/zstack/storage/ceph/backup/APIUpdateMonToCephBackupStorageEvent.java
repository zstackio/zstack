package org.zstack.storage.ceph.backup;

import org.zstack.header.message.APIEvent;

/**
 * Created by Mei Lei on 6/6/2016.
 */
public class APIUpdateMonToCephBackupStorageEvent extends APIEvent {
    private CephBackupStorageInventory inventory;

    public APIUpdateMonToCephBackupStorageEvent() {
    }

    public APIUpdateMonToCephBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public CephBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
