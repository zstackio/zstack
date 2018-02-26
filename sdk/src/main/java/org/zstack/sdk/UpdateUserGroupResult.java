package org.zstack.sdk;

import org.zstack.sdk.UserGroupInventory;

public class UpdateUserGroupResult {
    public UserGroupInventory inventory;
    public void setInventory(UserGroupInventory inventory) {
        this.inventory = inventory;
    }
    public UserGroupInventory getInventory() {
        return this.inventory;
    }

}
