package org.zstack.sdk;

import org.zstack.sdk.VmQgaState;

public class VmQgaInventory  {

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public VmQgaState state;
    public void setState(VmQgaState state) {
        this.state = state;
    }
    public VmQgaState getState() {
        return this.state;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public java.lang.String Platform;
    public void setPlatform(java.lang.String Platform) {
        this.Platform = Platform;
    }
    public java.lang.String getPlatform() {
        return this.Platform;
    }

    public java.lang.String OsType;
    public void setOsType(java.lang.String OsType) {
        this.OsType = OsType;
    }
    public java.lang.String getOsType() {
        return this.OsType;
    }

}
