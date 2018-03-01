package org.zstack.sdk;

import org.zstack.sdk.EcsImageInventory;

public class CreateEcsImageFromEcsSnapshotResult {
    public EcsImageInventory inventory;
    public void setInventory(EcsImageInventory inventory) {
        this.inventory = inventory;
    }
    public EcsImageInventory getInventory() {
        return this.inventory;
    }

}
