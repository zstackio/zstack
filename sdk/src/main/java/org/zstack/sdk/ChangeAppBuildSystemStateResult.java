package org.zstack.sdk;

import org.zstack.sdk.AppBuildSystemInventory;

public class ChangeAppBuildSystemStateResult {
    public AppBuildSystemInventory inventory;
    public void setInventory(AppBuildSystemInventory inventory) {
        this.inventory = inventory;
    }
    public AppBuildSystemInventory getInventory() {
        return this.inventory;
    }

}
