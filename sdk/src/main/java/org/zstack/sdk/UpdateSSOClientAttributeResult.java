package org.zstack.sdk;

import org.zstack.sdk.SSOClientAttributeInventory;

public class UpdateSSOClientAttributeResult {
    public SSOClientAttributeInventory inventory;
    public void setInventory(SSOClientAttributeInventory inventory) {
        this.inventory = inventory;
    }
    public SSOClientAttributeInventory getInventory() {
        return this.inventory;
    }

}
