package org.zstack.sdk.license.entity;



public class LicenseUsageResult  {

    public java.lang.String module;
    public void setModule(java.lang.String module) {
        this.module = module;
    }
    public java.lang.String getModule() {
        return this.module;
    }

    public java.lang.String primaryQuotaType;
    public void setPrimaryQuotaType(java.lang.String primaryQuotaType) {
        this.primaryQuotaType = primaryQuotaType;
    }
    public java.lang.String getPrimaryQuotaType() {
        return this.primaryQuotaType;
    }

    public java.util.List usages;
    public void setUsages(java.util.List usages) {
        this.usages = usages;
    }
    public java.util.List getUsages() {
        return this.usages;
    }

}
