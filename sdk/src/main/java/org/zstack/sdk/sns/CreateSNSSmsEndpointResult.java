package org.zstack.sdk.sns;

import org.zstack.sdk.sns.SNSSmsEndpointInventory;

public class CreateSNSSmsEndpointResult {
    public SNSSmsEndpointInventory inventory;
    public void setInventory(SNSSmsEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSSmsEndpointInventory getInventory() {
        return this.inventory;
    }

}
