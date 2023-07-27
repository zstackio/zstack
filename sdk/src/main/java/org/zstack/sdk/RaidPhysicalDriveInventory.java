package org.zstack.sdk;

import org.zstack.sdk.LocateStatus;

public class RaidPhysicalDriveInventory extends org.zstack.sdk.PhysicalDriveInventory {

    public java.lang.String raidLevel;
    public void setRaidLevel(java.lang.String raidLevel) {
        this.raidLevel = raidLevel;
    }
    public java.lang.String getRaidLevel() {
        return this.raidLevel;
    }

    public java.lang.String raidControllerUuid;
    public void setRaidControllerUuid(java.lang.String raidControllerUuid) {
        this.raidControllerUuid = raidControllerUuid;
    }
    public java.lang.String getRaidControllerUuid() {
        return this.raidControllerUuid;
    }

    public java.lang.String lunUuid;
    public void setLunUuid(java.lang.String lunUuid) {
        this.lunUuid = lunUuid;
    }
    public java.lang.String getLunUuid() {
        return this.lunUuid;
    }

    public java.lang.Integer deviceId;
    public void setDeviceId(java.lang.Integer deviceId) {
        this.deviceId = deviceId;
    }
    public java.lang.Integer getDeviceId() {
        return this.deviceId;
    }

    public java.lang.Integer enclosureDeviceId;
    public void setEnclosureDeviceId(java.lang.Integer enclosureDeviceId) {
        this.enclosureDeviceId = enclosureDeviceId;
    }
    public java.lang.Integer getEnclosureDeviceId() {
        return this.enclosureDeviceId;
    }

    public java.lang.Integer slotNumber;
    public void setSlotNumber(java.lang.Integer slotNumber) {
        this.slotNumber = slotNumber;
    }
    public java.lang.Integer getSlotNumber() {
        return this.slotNumber;
    }

    public java.lang.Integer diskGroup;
    public void setDiskGroup(java.lang.Integer diskGroup) {
        this.diskGroup = diskGroup;
    }
    public java.lang.Integer getDiskGroup() {
        return this.diskGroup;
    }

    public java.lang.String driveState;
    public void setDriveState(java.lang.String driveState) {
        this.driveState = driveState;
    }
    public java.lang.String getDriveState() {
        return this.driveState;
    }

    public LocateStatus locateStatus;
    public void setLocateStatus(LocateStatus locateStatus) {
        this.locateStatus = locateStatus;
    }
    public LocateStatus getLocateStatus() {
        return this.locateStatus;
    }

    public java.lang.Integer rotationRate;
    public void setRotationRate(java.lang.Integer rotationRate) {
        this.rotationRate = rotationRate;
    }
    public java.lang.Integer getRotationRate() {
        return this.rotationRate;
    }

}
