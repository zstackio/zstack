package org.zstack.sdk.license.api.server;

import org.zstack.sdk.license.header.server.LicenseAuthorizedCapacityInventory;

public class RequestLicenseCapacityResult {
    public LicenseAuthorizedCapacityInventory inventory;
    public void setInventory(LicenseAuthorizedCapacityInventory inventory) {
        this.inventory = inventory;
    }
    public LicenseAuthorizedCapacityInventory getInventory() {
        return this.inventory;
    }

}
