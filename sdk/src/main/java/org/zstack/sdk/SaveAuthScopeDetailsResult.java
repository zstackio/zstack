package org.zstack.sdk;

import org.zstack.sdk.AuthScopeDetailsInventory;

public class SaveAuthScopeDetailsResult {
    public AuthScopeDetailsInventory inventory;
    public void setInventory(AuthScopeDetailsInventory inventory) {
        this.inventory = inventory;
    }
    public AuthScopeDetailsInventory getInventory() {
        return this.inventory;
    }

}
