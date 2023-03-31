package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIFlattenVmInstanceEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public APIFlattenVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIFlattenVmInstanceEvent() {
        super();
    }
}
