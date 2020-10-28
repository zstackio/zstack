package org.zstack.sdk;

import org.zstack.sdk.AuthClientDetailsInventory;

public class SaveAuthClientDetailsResult {
    public AuthClientDetailsInventory inventory;
    public void setInventory(AuthClientDetailsInventory inventory) {
        this.inventory = inventory;
    }
    public AuthClientDetailsInventory getInventory() {
        return this.inventory;
    }

}
