package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 2/26/2016.
 */
public class APIDeleteVmStaticIpEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIDeleteVmStaticIpEvent() {
    }

    public APIDeleteVmStaticIpEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
