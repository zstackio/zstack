package org.zstack.sdk.license.entity.server;



public class LicenseAuthorizedCapacityClientAddOnUsageView  {

    public java.lang.String module;
    public void setModule(java.lang.String module) {
        this.module = module;
    }
    public java.lang.String getModule() {
        return this.module;
    }

    public long used;
    public void setUsed(long used) {
        this.used = used;
    }
    public long getUsed() {
        return this.used;
    }

    public java.util.List usageDetails;
    public void setUsageDetails(java.util.List usageDetails) {
        this.usageDetails = usageDetails;
    }
    public java.util.List getUsageDetails() {
        return this.usageDetails;
    }

}
