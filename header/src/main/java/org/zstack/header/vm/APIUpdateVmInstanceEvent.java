package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 6/14/2015.
 */
public class APIUpdateVmInstanceEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIUpdateVmInstanceEvent() {
    }

    public APIUpdateVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
