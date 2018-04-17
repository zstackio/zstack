package org.zstack.sdk;

import org.zstack.sdk.DahoConnectionInventory;

public class UpdateDahoDataCenterConnectionResult {
    public DahoConnectionInventory inventory;
    public void setInventory(DahoConnectionInventory inventory) {
        this.inventory = inventory;
    }
    public DahoConnectionInventory getInventory() {
        return this.inventory;
    }

}
