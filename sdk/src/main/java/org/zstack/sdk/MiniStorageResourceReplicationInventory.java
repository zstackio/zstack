package org.zstack.sdk;

import org.zstack.sdk.ReplicationState;
import org.zstack.sdk.ReplicationRole;
import org.zstack.sdk.ReplicationNetworkStatus;
import org.zstack.sdk.ReplicationDiskStatus;

public class MiniStorageResourceReplicationInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String primaryStorageUuid;
    public void setPrimaryStorageUuid(java.lang.String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
    public java.lang.String getPrimaryStorageUuid() {
        return this.primaryStorageUuid;
    }

    public ReplicationState state;
    public void setState(ReplicationState state) {
        this.state = state;
    }
    public ReplicationState getState() {
        return this.state;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public ReplicationRole role;
    public void setRole(ReplicationRole role) {
        this.role = role;
    }
    public ReplicationRole getRole() {
        return this.role;
    }

    public ReplicationNetworkStatus networkStatus;
    public void setNetworkStatus(ReplicationNetworkStatus networkStatus) {
        this.networkStatus = networkStatus;
    }
    public ReplicationNetworkStatus getNetworkStatus() {
        return this.networkStatus;
    }

    public ReplicationDiskStatus diskStatus;
    public void setDiskStatus(ReplicationDiskStatus diskStatus) {
        this.diskStatus = diskStatus;
    }
    public ReplicationDiskStatus getDiskStatus() {
        return this.diskStatus;
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
