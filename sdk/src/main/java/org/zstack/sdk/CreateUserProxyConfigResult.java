package org.zstack.sdk;

import org.zstack.sdk.UserProxyConfigInventory;

public class CreateUserProxyConfigResult {
    public UserProxyConfigInventory inventory;
    public void setInventory(UserProxyConfigInventory inventory) {
        this.inventory = inventory;
    }
    public UserProxyConfigInventory getInventory() {
        return this.inventory;
    }

}
