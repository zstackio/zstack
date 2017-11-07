package org.zstack.sdk;

import org.zstack.sdk.SNSApplicationEndpointInventory;

public class CreateSNSApplicationEndpointResult {
    public SNSApplicationEndpointInventory inventory;
    public void setInventory(SNSApplicationEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSApplicationEndpointInventory getInventory() {
        return this.inventory;
    }

}
