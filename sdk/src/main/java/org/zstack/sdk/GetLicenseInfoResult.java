package org.zstack.sdk;

import org.zstack.sdk.LicenseInventory;

public class GetLicenseInfoResult {
    public LicenseInventory inventory;
    public void setInventory(LicenseInventory inventory) {
        this.inventory = inventory;
    }
    public LicenseInventory getInventory() {
        return this.inventory;
    }

    public java.util.List additions;
    public void setAdditions(java.util.List additions) {
        this.additions = additions;
    }
    public java.util.List getAdditions() {
        return this.additions;
    }

}
