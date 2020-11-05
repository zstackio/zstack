package org.zstack.sdk;

import java.sql.Timestamp;

public class LoadBalancerServerGroupServerIpInventory {
    public java.lang.Long id;
    public void setId(java.lang.Long id) {
        this.id = id;
    }
    public java.lang.Long getId() {
        return this.id;
    }

    public java.lang.String description;
    public java.lang.String  getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public java.lang.String ipAddress;
    public void setIpAddress(java.lang.String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public java.lang.String getIpAddress() {
        return this.ipAddress;
    }

    public java.lang.String serverGroupUuid;
    public void setServerGroupUuid(java.lang.String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
    public java.lang.String getServerGroupUuid() {
        return this.serverGroupUuid;
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
