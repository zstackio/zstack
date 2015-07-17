package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/16/2015.
 */
public class APIChangeInstanceOfferingEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIChangeInstanceOfferingEvent() {
    }

    public APIChangeInstanceOfferingEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
