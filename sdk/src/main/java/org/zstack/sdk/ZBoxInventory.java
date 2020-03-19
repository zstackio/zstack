package org.zstack.sdk;

import org.zstack.sdk.ZBoxState;
import org.zstack.sdk.ZBoxStatus;

public class ZBoxInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public ZBoxState state;
    public void setState(ZBoxState state) {
        this.state = state;
    }
    public ZBoxState getState() {
        return this.state;
    }

    public ZBoxStatus status;
    public void setStatus(ZBoxStatus status) {
        this.status = status;
    }
    public ZBoxStatus getStatus() {
        return this.status;
    }

    public java.util.List locationRefs;
    public void setLocationRefs(java.util.List locationRefs) {
        this.locationRefs = locationRefs;
    }
    public java.util.List getLocationRefs() {
        return this.locationRefs;
    }

    public java.lang.String mountPath;
    public void setMountPath(java.lang.String mountPath) {
        this.mountPath = mountPath;
    }
    public java.lang.String getMountPath() {
        return this.mountPath;
    }

    public long totalCapacity;
    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    public long getTotalCapacity() {
        return this.totalCapacity;
    }

    public long availableCapacity;
    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
    public long getAvailableCapacity() {
        return this.availableCapacity;
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
