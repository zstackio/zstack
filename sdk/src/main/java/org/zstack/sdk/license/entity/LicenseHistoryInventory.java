package org.zstack.sdk.license.entity;



public class LicenseHistoryInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.Long quota;
    public void setQuota(java.lang.Long quota) {
        this.quota = quota;
    }
    public java.lang.Long getQuota() {
        return this.quota;
    }

    public java.lang.String quotaType;
    public void setQuotaType(java.lang.String quotaType) {
        this.quotaType = quotaType;
    }
    public java.lang.String getQuotaType() {
        return this.quotaType;
    }

    public java.lang.String expiredDate;
    public void setExpiredDate(java.lang.String expiredDate) {
        this.expiredDate = expiredDate;
    }
    public java.lang.String getExpiredDate() {
        return this.expiredDate;
    }

    public java.lang.String issuedDate;
    public void setIssuedDate(java.lang.String issuedDate) {
        this.issuedDate = issuedDate;
    }
    public java.lang.String getIssuedDate() {
        return this.issuedDate;
    }

    public java.lang.String uploadDate;
    public void setUploadDate(java.lang.String uploadDate) {
        this.uploadDate = uploadDate;
    }
    public java.lang.String getUploadDate() {
        return this.uploadDate;
    }

    public java.lang.String licenseType;
    public void setLicenseType(java.lang.String licenseType) {
        this.licenseType = licenseType;
    }
    public java.lang.String getLicenseType() {
        return this.licenseType;
    }

    public java.lang.String prodInfo;
    public void setProdInfo(java.lang.String prodInfo) {
        this.prodInfo = prodInfo;
    }
    public java.lang.String getProdInfo() {
        return this.prodInfo;
    }

    public java.lang.String userName;
    public void setUserName(java.lang.String userName) {
        this.userName = userName;
    }
    public java.lang.String getUserName() {
        return this.userName;
    }

    public java.lang.String hash;
    public void setHash(java.lang.String hash) {
        this.hash = hash;
    }
    public java.lang.String getHash() {
        return this.hash;
    }

    public java.lang.String source;
    public void setSource(java.lang.String source) {
        this.source = source;
    }
    public java.lang.String getSource() {
        return this.source;
    }

    public java.lang.String cause;
    public void setCause(java.lang.String cause) {
        this.cause = cause;
    }
    public java.lang.String getCause() {
        return this.cause;
    }

    public java.lang.String managementNodeUuid;
    public void setManagementNodeUuid(java.lang.String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
    public java.lang.String getManagementNodeUuid() {
        return this.managementNodeUuid;
    }

    public long mergedTo;
    public void setMergedTo(long mergedTo) {
        this.mergedTo = mergedTo;
    }
    public long getMergedTo() {
        return this.mergedTo;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public boolean expired;
    public void setExpired(boolean expired) {
        this.expired = expired;
    }
    public boolean getExpired() {
        return this.expired;
    }

    @Deprecated
    public java.lang.Integer cpuNum;
    public void setCpuNum(java.lang.Integer cpuNum) {
        this.cpuNum = cpuNum;
    }
    public java.lang.Integer getCpuNum() {
        return this.cpuNum;
    }

    @Deprecated
    public java.lang.Integer hostNum;
    public void setHostNum(java.lang.Integer hostNum) {
        this.hostNum = hostNum;
    }
    public java.lang.Integer getHostNum() {
        return this.hostNum;
    }

    @Deprecated
    public java.lang.Integer vmNum;
    public void setVmNum(java.lang.Integer vmNum) {
        this.vmNum = vmNum;
    }
    public java.lang.Integer getVmNum() {
        return this.vmNum;
    }

    @Deprecated
    public java.lang.Integer capacity;
    public void setCapacity(java.lang.Integer capacity) {
        this.capacity = capacity;
    }
    public java.lang.Integer getCapacity() {
        return this.capacity;
    }

}
