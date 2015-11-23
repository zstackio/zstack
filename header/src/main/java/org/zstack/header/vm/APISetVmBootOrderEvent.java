package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 11/22/2015.
 */
public class APISetVmBootOrderEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APISetVmBootOrderEvent() {
    }

    public APISetVmBootOrderEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
