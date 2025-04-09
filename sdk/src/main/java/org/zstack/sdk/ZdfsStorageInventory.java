package org.zstack.sdk;

import org.zstack.sdk.StorageType;

public class ZdfsStorageInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String endPoint;
    public void setEndPoint(java.lang.String endPoint) {
        this.endPoint = endPoint;
    }
    public java.lang.String getEndPoint() {
        return this.endPoint;
    }

    public java.lang.String accessKey;
    public void setAccessKey(java.lang.String accessKey) {
        this.accessKey = accessKey;
    }
    public java.lang.String getAccessKey() {
        return this.accessKey;
    }

    public java.lang.String secretKey;
    public void setSecretKey(java.lang.String secretKey) {
        this.secretKey = secretKey;
    }
    public java.lang.String getSecretKey() {
        return this.secretKey;
    }

    public StorageType type;
    public void setType(StorageType type) {
        this.type = type;
    }
    public StorageType getType() {
        return this.type;
    }

    public java.lang.Long usedCapacity;
    public void setUsedCapacity(java.lang.Long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }
    public java.lang.Long getUsedCapacity() {
        return this.usedCapacity;
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
