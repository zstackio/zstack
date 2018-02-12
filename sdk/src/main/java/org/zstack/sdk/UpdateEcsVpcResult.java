package org.zstack.sdk;

import org.zstack.sdk.EcsVpcInventory;

public class UpdateEcsVpcResult {
    public EcsVpcInventory inventory;
    public void setInventory(EcsVpcInventory inventory) {
        this.inventory = inventory;
    }
    public EcsVpcInventory getInventory() {
        return this.inventory;
    }

}
