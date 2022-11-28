package org.zstack.sdk.disasterrecovery;

import org.zstack.sdk.disasterrecovery.MirrorCdpTaskScheduleJobStatus;

public class MirrorCdpTaskScheduleJobInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String groupName;
    public void setGroupName(java.lang.String groupName) {
        this.groupName = groupName;
    }
    public java.lang.String getGroupName() {
        return this.groupName;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public MirrorCdpTaskScheduleJobStatus status;
    public void setStatus(MirrorCdpTaskScheduleJobStatus status) {
        this.status = status;
    }
    public MirrorCdpTaskScheduleJobStatus getStatus() {
        return this.status;
    }

    public java.util.List taskRefs;
    public void setTaskRefs(java.util.List taskRefs) {
        this.taskRefs = taskRefs;
    }
    public java.util.List getTaskRefs() {
        return this.taskRefs;
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
