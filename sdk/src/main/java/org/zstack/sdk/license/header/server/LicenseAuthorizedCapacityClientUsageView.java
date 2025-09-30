package org.zstack.sdk.license.header.server;

import org.zstack.sdk.license.header.server.LicenseAuthorizedNodeInventory;

public class LicenseAuthorizedCapacityClientUsageView  {

    public java.lang.String clientAppId;
    public void setClientAppId(java.lang.String clientAppId) {
        this.clientAppId = clientAppId;
    }
    public java.lang.String getClientAppId() {
        return this.clientAppId;
    }

    public java.lang.String clientAuthorizedNodeUuid;
    public void setClientAuthorizedNodeUuid(java.lang.String clientAuthorizedNodeUuid) {
        this.clientAuthorizedNodeUuid = clientAuthorizedNodeUuid;
    }
    public java.lang.String getClientAuthorizedNodeUuid() {
        return this.clientAuthorizedNodeUuid;
    }

    public LicenseAuthorizedNodeInventory clientInventory;
    public void setClientInventory(LicenseAuthorizedNodeInventory clientInventory) {
        this.clientInventory = clientInventory;
    }
    public LicenseAuthorizedNodeInventory getClientInventory() {
        return this.clientInventory;
    }

    public long platformUsed;
    public void setPlatformUsed(long platformUsed) {
        this.platformUsed = platformUsed;
    }
    public long getPlatformUsed() {
        return this.platformUsed;
    }

    public java.util.List platformUsageDetails;
    public void setPlatformUsageDetails(java.util.List platformUsageDetails) {
        this.platformUsageDetails = platformUsageDetails;
    }
    public java.util.List getPlatformUsageDetails() {
        return this.platformUsageDetails;
    }

    public java.util.List addOns;
    public void setAddOns(java.util.List addOns) {
        this.addOns = addOns;
    }
    public java.util.List getAddOns() {
        return this.addOns;
    }

}
