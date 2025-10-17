package org.zstack.sdk;



public class FileVerificationRecordsInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String fileVerificationUuid;
    public void setFileVerificationUuid(java.lang.String fileVerificationUuid) {
        this.fileVerificationUuid = fileVerificationUuid;
    }
    public java.lang.String getFileVerificationUuid() {
        return this.fileVerificationUuid;
    }

    public java.lang.String path;
    public void setPath(java.lang.String path) {
        this.path = path;
    }
    public java.lang.String getPath() {
        return this.path;
    }

    public java.lang.String node;
    public void setNode(java.lang.String node) {
        this.node = node;
    }
    public java.lang.String getNode() {
        return this.node;
    }

    public java.lang.String currentDigest;
    public void setCurrentDigest(java.lang.String currentDigest) {
        this.currentDigest = currentDigest;
    }
    public java.lang.String getCurrentDigest() {
        return this.currentDigest;
    }

    public java.lang.String targetDigest;
    public void setTargetDigest(java.lang.String targetDigest) {
        this.targetDigest = targetDigest;
    }
    public java.lang.String getTargetDigest() {
        return this.targetDigest;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

    public boolean recoverFlag;
    public void setRecoverFlag(boolean recoverFlag) {
        this.recoverFlag = recoverFlag;
    }
    public boolean getRecoverFlag() {
        return this.recoverFlag;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

}
