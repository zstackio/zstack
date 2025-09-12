package org.zstack.sdk;

import org.zstack.sdk.SAML2ClientInventory;

public class UpdateSAML2ClientResult {
    public SAML2ClientInventory inventory;
    public void setInventory(SAML2ClientInventory inventory) {
        this.inventory = inventory;
    }
    public SAML2ClientInventory getInventory() {
        return this.inventory;
    }

}
