package org.zstack.sdk;

import org.zstack.sdk.UserProxyConfigResourceRefInventory;

public class AddProxyToResourceResult {
    public UserProxyConfigResourceRefInventory inventory;
    public void setInventory(UserProxyConfigResourceRefInventory inventory) {
        this.inventory = inventory;
    }
    public UserProxyConfigResourceRefInventory getInventory() {
        return this.inventory;
    }

}
