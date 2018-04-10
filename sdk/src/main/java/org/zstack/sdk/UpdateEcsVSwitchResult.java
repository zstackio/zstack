package org.zstack.sdk;

import org.zstack.sdk.EcsVSwitchInventory;

public class UpdateEcsVSwitchResult {
    public EcsVSwitchInventory inventory;
    public void setInventory(EcsVSwitchInventory inventory) {
        this.inventory = inventory;
    }
    public EcsVSwitchInventory getInventory() {
        return this.inventory;
    }

}
