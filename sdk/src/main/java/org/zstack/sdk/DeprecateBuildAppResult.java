package org.zstack.sdk;

import org.zstack.sdk.BuildApplicationInventory;

public class DeprecateBuildAppResult {
    public BuildApplicationInventory inventory;
    public void setInventory(BuildApplicationInventory inventory) {
        this.inventory = inventory;
    }
    public BuildApplicationInventory getInventory() {
        return this.inventory;
    }

}
