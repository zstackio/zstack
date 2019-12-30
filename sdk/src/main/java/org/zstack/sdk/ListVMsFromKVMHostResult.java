package org.zstack.sdk;



public class ListVMsFromKVMHostResult {
    public java.util.List inventories;
    public void setInventories(java.util.List inventories) {
        this.inventories = inventories;
    }
    public java.util.List getInventories() {
        return this.inventories;
    }

    public java.lang.String libvirtVersion;
    public void setLibvirtVersion(java.lang.String libvirtVersion) {
        this.libvirtVersion = libvirtVersion;
    }
    public java.lang.String getLibvirtVersion() {
        return this.libvirtVersion;
    }

    public java.lang.String qemuVersion;
    public void setQemuVersion(java.lang.String qemuVersion) {
        this.qemuVersion = qemuVersion;
    }
    public java.lang.String getQemuVersion() {
        return this.qemuVersion;
    }

    public java.util.Map v2vCaps;
    public void setV2vCaps(java.util.Map v2vCaps) {
        this.v2vCaps = v2vCaps;
    }
    public java.util.Map getV2vCaps() {
        return v2vCaps;
    }
}
