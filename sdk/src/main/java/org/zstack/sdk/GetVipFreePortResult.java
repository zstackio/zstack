package org.zstack.sdk;

import org.zstack.sdk.GetVipFreePortInventory;

public class GetVipFreePortResult {
    public GetVipFreePortInventory inventory;
    public void setInventory(GetVipFreePortInventory inventory) {
        this.inventory = inventory;
    }
    public GetVipFreePortInventory getInventory() {
        return this.inventory;
    }

}
