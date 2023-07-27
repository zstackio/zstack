package org.zstack.sdk;



public class RaidLunInventory extends org.zstack.sdk.LunInventory {

    public java.lang.String controllerUuid;
    public void setControllerUuid(java.lang.String controllerUuid) {
        this.controllerUuid = controllerUuid;
    }
    public java.lang.String getControllerUuid() {
        return this.controllerUuid;
    }

    public java.lang.Integer diskGroup;
    public void setDiskGroup(java.lang.Integer diskGroup) {
        this.diskGroup = diskGroup;
    }
    public java.lang.Integer getDiskGroup() {
        return this.diskGroup;
    }

    public java.lang.String healthState;
    public void setHealthState(java.lang.String healthState) {
        this.healthState = healthState;
    }
    public java.lang.String getHealthState() {
        return this.healthState;
    }

    public java.util.List raidPhysicalDrives;
    public void setRaidPhysicalDrives(java.util.List raidPhysicalDrives) {
        this.raidPhysicalDrives = raidPhysicalDrives;
    }
    public java.util.List getRaidPhysicalDrives() {
        return this.raidPhysicalDrives;
    }

}
