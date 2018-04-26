package org.zstack.sdk.iam2.api;

import org.zstack.sdk.SessionInventory;

public class LoginIAM2ProjectResult {
    public SessionInventory inventory;
    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }
    public SessionInventory getInventory() {
        return this.inventory;
    }

}
