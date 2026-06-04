package org.zstack.sdk;

import org.zstack.sdk.NativeClusterInventory;

public class UpdateNativeClusterZakuHealthStatusResult {
    public NativeClusterInventory inventory;
    public void setInventory(NativeClusterInventory inventory) {
        this.inventory = inventory;
    }
    public NativeClusterInventory getInventory() {
        return this.inventory;
    }

}
