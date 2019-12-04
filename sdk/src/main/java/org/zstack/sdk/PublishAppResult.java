package org.zstack.sdk;

import org.zstack.sdk.PublishAppInventory;

public class PublishAppResult {
    public PublishAppInventory inventory;
    public void setInventory(PublishAppInventory inventory) {
        this.inventory = inventory;
    }
    public PublishAppInventory getInventory() {
        return this.inventory;
    }

}
