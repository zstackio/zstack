package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by root on 8/2/16.
 */
public class APIDeleteVmConsolePasswordEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIDeleteVmConsolePasswordEvent() {

    }

    public APIDeleteVmConsolePasswordEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }
}
