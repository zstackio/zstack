package org.zstack.storage.ceph.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Mei Lei on 6/6/2016.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateCephBackupStorageMonEvent extends APIEvent {
    private CephBackupStorageInventory inventory;

    public APIUpdateCephBackupStorageMonEvent() {
    }

    public APIUpdateCephBackupStorageMonEvent(String apiId) {
        super(apiId);
    }

    public CephBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
