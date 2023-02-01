package org.zstack.sdk;

import org.zstack.sdk.GuestToolsState;

public class GuestToolsStateInventory  {

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public GuestToolsState state;
    public void setState(GuestToolsState state) {
        this.state = state;
    }
    public GuestToolsState getState() {
        return this.state;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public java.lang.String platform;
    public void setPlatform(java.lang.String platform) {
        this.platform = platform;
    }
    public java.lang.String getPlatform() {
        return this.platform;
    }

    public java.lang.String osType;
    public void setOsType(java.lang.String osType) {
        this.osType = osType;
    }
    public java.lang.String getOsType() {
        return this.osType;
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
