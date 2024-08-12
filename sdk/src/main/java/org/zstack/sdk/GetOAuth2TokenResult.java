package org.zstack.sdk;

import org.zstack.sdk.OAuth2TokenInventory;
import org.zstack.sdk.SSOServerTokenInventory;

public class GetOAuth2TokenResult {
    public OAuth2TokenInventory inventory;
    public void setInventory(OAuth2TokenInventory inventory) {
        this.inventory = inventory;
    }
    public OAuth2TokenInventory getInventory() {
        return this.inventory;
    }

    public SSOServerTokenInventory serverTokenInventory;
    public void setServerTokenInventory(SSOServerTokenInventory serverTokenInventory) {
        this.serverTokenInventory = serverTokenInventory;
    }
    public SSOServerTokenInventory getServerTokenInventory() {
        return this.serverTokenInventory;
    }

}
