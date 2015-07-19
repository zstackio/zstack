package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/18/2015.
 */
public class APIDetachNicFromVmEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public APIDetachNicFromVmEvent() {
    }

    public APIDetachNicFromVmEvent(String apiId) {
        super(apiId);
    }
}
