package org.zstack.sdk;

import org.zstack.sdk.UserTagInventory;

public class CreateUserTagResult {
    public UserTagInventory inventory;
    public void setInventory(UserTagInventory inventory) {
        this.inventory = inventory;
    }
    public UserTagInventory getInventory() {
        return this.inventory;
    }

}
