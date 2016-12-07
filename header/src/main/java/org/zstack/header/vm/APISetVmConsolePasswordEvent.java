package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by root on 7/29/16.
 */
@RestResponse(allTo = "inventory")
public class APISetVmConsolePasswordEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APISetVmConsolePasswordEvent() {

    }

    public APISetVmConsolePasswordEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

}
