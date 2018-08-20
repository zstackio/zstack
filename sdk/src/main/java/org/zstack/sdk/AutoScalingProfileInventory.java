package org.zstack.sdk;

import org.zstack.sdk.AutoScalingProfileState;
import org.zstack.sdk.AutoScalingProfileStatus;

public class AutoScalingProfileInventory  {

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.Long cooldown;
    public void setCooldown(java.lang.Long cooldown) {
        this.cooldown = cooldown;
    }
    public java.lang.Long getCooldown() {
        return this.cooldown;
    }

    public AutoScalingProfileState state;
    public void setState(AutoScalingProfileState state) {
        this.state = state;
    }
    public AutoScalingProfileState getState() {
        return this.state;
    }

    public AutoScalingProfileStatus status;
    public void setStatus(AutoScalingProfileStatus status) {
        this.status = status;
    }
    public AutoScalingProfileStatus getStatus() {
        return this.status;
    }

    public java.util.List systemTags;
    public void setSystemTags(java.util.List systemTags) {
        this.systemTags = systemTags;
    }
    public java.util.List getSystemTags() {
        return this.systemTags;
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

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.util.List attachedGroups;
    public void setAttachedGroups(java.util.List attachedGroups) {
        this.attachedGroups = attachedGroups;
    }
    public java.util.List getAttachedGroups() {
        return this.attachedGroups;
    }

}
