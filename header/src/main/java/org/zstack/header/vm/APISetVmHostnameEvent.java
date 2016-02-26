package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 2/26/2016.
 */
public class APISetVmHostnameEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APISetVmHostnameEvent() {
    }

    public APISetVmHostnameEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
