package org.zstack.sdk.license.entity.server;

import org.zstack.sdk.license.entity.server.LicenseAuthorizedNodeInventory;
import org.zstack.sdk.LicenseInventory;

public class TotalLicenseAuthorizedCapacityView  {

    public java.lang.String serverAppId;
    public void setServerAppId(java.lang.String serverAppId) {
        this.serverAppId = serverAppId;
    }
    public java.lang.String getServerAppId() {
        return this.serverAppId;
    }

    public java.lang.String serverAuthorizedNodeUuid;
    public void setServerAuthorizedNodeUuid(java.lang.String serverAuthorizedNodeUuid) {
        this.serverAuthorizedNodeUuid = serverAuthorizedNodeUuid;
    }
    public java.lang.String getServerAuthorizedNodeUuid() {
        return this.serverAuthorizedNodeUuid;
    }

    public LicenseAuthorizedNodeInventory serverInventory;
    public void setServerInventory(LicenseAuthorizedNodeInventory serverInventory) {
        this.serverInventory = serverInventory;
    }
    public LicenseAuthorizedNodeInventory getServerInventory() {
        return this.serverInventory;
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

    public java.util.Map extensions;
    public void setExtensions(java.util.Map extensions) {
        this.extensions = extensions;
    }
    public java.util.Map getExtensions() {
        return this.extensions;
    }

}
