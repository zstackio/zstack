package org.zstack.sdk.license.api.server;

import org.zstack.sdk.license.header.server.LicenseAuthorizedNodeInventory;
import org.zstack.sdk.LicenseInventory;

public class GetLicenseNodeUsageDetailsResult {
    public LicenseAuthorizedNodeInventory nodeInventory;
    public void setNodeInventory(LicenseAuthorizedNodeInventory nodeInventory) {
        this.nodeInventory = nodeInventory;
    }
    public LicenseAuthorizedNodeInventory getNodeInventory() {
        return this.nodeInventory;
    }

    public LicenseInventory platformLicense;
    public void setPlatformLicense(LicenseInventory platformLicense) {
        this.platformLicense = platformLicense;
    }
    public LicenseInventory getPlatformLicense() {
        return this.platformLicense;
    }

    public java.util.List addOns;
    public void setAddOns(java.util.List addOns) {
        this.addOns = addOns;
    }
    public java.util.List getAddOns() {
        return this.addOns;
    }

}
