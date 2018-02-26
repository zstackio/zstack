package org.zstack.sdk;

import org.zstack.sdk.EcsInstanceInventory;

public class CreateEcsInstanceFromEcsImageResult {
    public EcsInstanceInventory inventory;
    public void setInventory(EcsInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public EcsInstanceInventory getInventory() {
        return this.inventory;
    }

}
