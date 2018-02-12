package org.zstack.sdk;

import org.zstack.sdk.EcsImageInventory;

public class CreateEcsImageFromLocalImageResult {
    public EcsImageInventory inventory;
    public void setInventory(EcsImageInventory inventory) {
        this.inventory = inventory;
    }
    public EcsImageInventory getInventory() {
        return this.inventory;
    }

}
