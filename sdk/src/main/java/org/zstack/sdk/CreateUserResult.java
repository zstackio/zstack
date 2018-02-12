package org.zstack.sdk;

import org.zstack.sdk.UserInventory;

public class CreateUserResult {
    public UserInventory inventory;
    public void setInventory(UserInventory inventory) {
        this.inventory = inventory;
    }
    public UserInventory getInventory() {
        return this.inventory;
    }

}
