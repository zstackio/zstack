package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by luchukun on 8/4/16.
 */
@RestResponse(allTo = "inventory")
public class APISetVmSshKeyEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APISetVmSshKeyEvent() {
    }

    public APISetVmSshKeyEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }
}
