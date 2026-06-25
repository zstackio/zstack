package org.zstack.sdk.license.entity.server;



public class LicenseAuthorizedCapacityInventory  {

    public java.lang.Long id;
    public void setId(java.lang.Long id) {
        this.id = id;
    }
    public java.lang.Long getId() {
        return this.id;
    }

    public java.lang.String nodeUuid;
    public void setNodeUuid(java.lang.String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }
    public java.lang.String getNodeUuid() {
        return this.nodeUuid;
    }

    public java.lang.String prodInfo;
    public void setProdInfo(java.lang.String prodInfo) {
        this.prodInfo = prodInfo;
    }
    public java.lang.String getProdInfo() {
        return this.prodInfo;
    }

    public java.lang.String quotaType;
    public void setQuotaType(java.lang.String quotaType) {
        this.quotaType = quotaType;
    }
    public java.lang.String getQuotaType() {
        return this.quotaType;
    }

    public java.lang.Long quota;
    public void setQuota(java.lang.Long quota) {
        this.quota = quota;
    }
    public java.lang.Long getQuota() {
        return this.quota;
    }

    public java.lang.String licenseType;
    public void setLicenseType(java.lang.String licenseType) {
        this.licenseType = licenseType;
    }
    public java.lang.String getLicenseType() {
        return this.licenseType;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String issueTime;
    public void setIssueTime(java.lang.String issueTime) {
        this.issueTime = issueTime;
    }
    public java.lang.String getIssueTime() {
        return this.issueTime;
    }

    public java.lang.String expireTime;
    public void setExpireTime(java.lang.String expireTime) {
        this.expireTime = expireTime;
    }
    public java.lang.String getExpireTime() {
        return this.expireTime;
    }

    public java.lang.Long localUsed;
    public void setLocalUsed(java.lang.Long localUsed) {
        this.localUsed = localUsed;
    }
    public java.lang.Long getLocalUsed() {
        return this.localUsed;
    }

    public java.lang.Long otherUsed;
    public void setOtherUsed(java.lang.Long otherUsed) {
        this.otherUsed = otherUsed;
    }
    public java.lang.Long getOtherUsed() {
        return this.otherUsed;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
