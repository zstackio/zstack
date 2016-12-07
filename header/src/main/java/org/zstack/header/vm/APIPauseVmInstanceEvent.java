package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by luchkun on 10/29/16.
 */
@RestResponse(allTo = "inventory")
public class APIPauseVmInstanceEvent extends APIEvent {

    private VmInstanceInventory inventory;

    public APIPauseVmInstanceEvent() {
        super(null);
    }

    public APIPauseVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
