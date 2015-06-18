package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 6/18/2015.
 */
public class APISyncPrimaryStorageCapacityEvent extends APIEvent {
    private PrimaryStorageInventory inventory;

    public APISyncPrimaryStorageCapacityEvent() {
    }

    public APISyncPrimaryStorageCapacityEvent(String apiId) {
        super(apiId);
    }

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
}
