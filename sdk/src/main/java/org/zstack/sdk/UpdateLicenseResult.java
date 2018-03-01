package org.zstack.sdk;

import org.zstack.sdk.LicenseInventory;

public class UpdateLicenseResult {
    public LicenseInventory inventory;
    public void setInventory(LicenseInventory inventory) {
        this.inventory = inventory;
    }
    public LicenseInventory getInventory() {
        return this.inventory;
    }

}
