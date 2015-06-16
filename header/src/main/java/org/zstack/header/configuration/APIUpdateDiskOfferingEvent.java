package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 6/15/2015.
 */
public class APIUpdateDiskOfferingEvent extends APIEvent {
    private DiskOfferingInventory inventory;

    public APIUpdateDiskOfferingEvent() {
    }

    public APIUpdateDiskOfferingEvent(String apiId) {
        super(apiId);
    }

    public DiskOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(DiskOfferingInventory inventory) {
        this.inventory = inventory;
    }
}
