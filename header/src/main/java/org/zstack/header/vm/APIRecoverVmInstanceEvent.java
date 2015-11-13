package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 11/12/2015.
 */
public class APIRecoverVmInstanceEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIRecoverVmInstanceEvent() {
    }

    public APIRecoverVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
