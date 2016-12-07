package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/18/2015.
 */
@RestResponse(allTo = "inventory")
public class APIDetachL3NetworkFromVmEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public APIDetachL3NetworkFromVmEvent() {
    }

    public APIDetachL3NetworkFromVmEvent(String apiId) {
        super(apiId);
    }
}
