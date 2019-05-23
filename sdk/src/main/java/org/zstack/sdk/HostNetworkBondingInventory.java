package org.zstack.sdk;



public class HostNetworkBondingInventory  {

    public java.lang.String bondingName;
    public void setBondingName(java.lang.String bondingName) {
        this.bondingName = bondingName;
    }
    public java.lang.String getBondingName() {
        return this.bondingName;
    }

    public java.lang.String mode;
    public void setMode(java.lang.String mode) {
        this.mode = mode;
    }
    public java.lang.String getMode() {
        return this.mode;
    }

    public java.lang.String xmitHashPolicy;
    public void setXmitHashPolicy(java.lang.String xmitHashPolicy) {
        this.xmitHashPolicy = xmitHashPolicy;
    }
    public java.lang.String getXmitHashPolicy() {
        return this.xmitHashPolicy;
    }

    public java.lang.String miiStatus;
    public void setMiiStatus(java.lang.String miiStatus) {
        this.miiStatus = miiStatus;
    }
    public java.lang.String getMiiStatus() {
        return this.miiStatus;
    }

    public java.lang.String mac;
    public void setMac(java.lang.String mac) {
        this.mac = mac;
    }
    public java.lang.String getMac() {
        return this.mac;
    }

    public java.util.List ipAddresses;
    public void setIpAddresses(java.util.List ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
    public java.util.List getIpAddresses() {
        return this.ipAddresses;
    }

    public long miimon;
    public void setMiimon(long miimon) {
        this.miimon = miimon;
    }
    public long getMiimon() {
        return this.miimon;
    }

    public boolean allSlavesActive;
    public void setAllSlavesActive(boolean allSlavesActive) {
        this.allSlavesActive = allSlavesActive;
    }
    public boolean getAllSlavesActive() {
        return this.allSlavesActive;
    }

    public java.util.List slaves;
    public void setSlaves(java.util.List slaves) {
        this.slaves = slaves;
    }
    public java.util.List getSlaves() {
        return this.slaves;
    }

}
