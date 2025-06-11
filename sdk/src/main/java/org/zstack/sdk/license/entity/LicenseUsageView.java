package org.zstack.sdk.license.entity;



public class LicenseUsageView  {

    public java.lang.String quotaType;
    public void setQuotaType(java.lang.String quotaType) {
        this.quotaType = quotaType;
    }
    public java.lang.String getQuotaType() {
        return this.quotaType;
    }

    public long quota;
    public void setQuota(long quota) {
        this.quota = quota;
    }
    public long getQuota() {
        return this.quota;
    }

    public long used;
    public void setUsed(long used) {
        this.used = used;
    }
    public long getUsed() {
        return this.used;
    }

    public long available;
    public void setAvailable(long available) {
        this.available = available;
    }
    public long getAvailable() {
        return this.available;
    }

}
