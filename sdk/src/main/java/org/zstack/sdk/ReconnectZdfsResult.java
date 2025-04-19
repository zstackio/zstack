package org.zstack.sdk;

import org.zstack.sdk.ZdfsInventory;

public class ReconnectZdfsResult {
    public ZdfsInventory inventory;
    public void setInventory(ZdfsInventory inventory) {
        this.inventory = inventory;
    }
    public ZdfsInventory getInventory() {
        return this.inventory;
    }

}
