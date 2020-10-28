package org.zstack.sdk;

import org.zstack.sdk.AuthInfoInventory;

public class CreateAuthInfoResult {
    public AuthInfoInventory inventory;
    public void setInventory(AuthInfoInventory inventory) {
        this.inventory = inventory;
    }
    public AuthInfoInventory getInventory() {
        return this.inventory;
    }

}
