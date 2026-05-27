package org.zstack.sdk;

import org.zstack.sdk.LogServerInventory;

public class AddLogServerResult {
    public LogServerInventory inventory;
    public void setInventory(LogServerInventory inventory) {
        this.inventory = inventory;
    }
    public LogServerInventory getInventory() {
        return this.inventory;
    }

}
