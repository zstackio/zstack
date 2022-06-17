package org.zstack.sdk;

import org.zstack.sdk.OAuthClientDetailsInventory;

public class CreateOAuthClientDetailsResult {
    public OAuthClientDetailsInventory inventory;
    public void setInventory(OAuthClientDetailsInventory inventory) {
        this.inventory = inventory;
    }
    public OAuthClientDetailsInventory getInventory() {
        return this.inventory;
    }

}
