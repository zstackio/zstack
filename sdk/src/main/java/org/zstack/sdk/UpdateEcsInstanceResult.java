package org.zstack.sdk;

import org.zstack.sdk.EcsInstanceInventory;

public class UpdateEcsInstanceResult {
    public EcsInstanceInventory inventory;
    public void setInventory(EcsInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public EcsInstanceInventory getInventory() {
        return this.inventory;
    }

}
