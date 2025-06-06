package org.zstack.sdk.license.api.server;

import org.zstack.sdk.LicenseAuthorizedNodeInventory;

public class UpgradeToLicenseServerResult {
    public LicenseAuthorizedNodeInventory inventory;
    public void setInventory(LicenseAuthorizedNodeInventory inventory) {
        this.inventory = inventory;
    }
    public LicenseAuthorizedNodeInventory getInventory() {
        return this.inventory;
    }

}
