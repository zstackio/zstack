package org.zstack.sdk;

public class SchedulerTriggerInventory  {

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

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String schedulerType;
    public void setSchedulerType(java.lang.String schedulerType) {
        this.schedulerType = schedulerType;
    }
    public java.lang.String getSchedulerType() {
        return this.schedulerType;
    }

    public java.lang.Integer schedulerInterval;
    public void setSchedulerInterval(java.lang.Integer schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }
    public java.lang.Integer getSchedulerInterval() {
        return this.schedulerInterval;
    }

    public java.lang.Integer repeatCount;
    public void setRepeatCount(java.lang.Integer repeatCount) {
        this.repeatCount = repeatCount;
    }
    public java.lang.Integer getRepeatCount() {
        return this.repeatCount;
    }

    public java.sql.Timestamp startTime;
    public void setStartTime(java.sql.Timestamp startTime) {
        this.startTime = startTime;
    }
    public java.sql.Timestamp getStartTime() {
        return this.startTime;
    }

    public java.sql.Timestamp stopTime;
    public void setStopTime(java.sql.Timestamp stopTime) {
        this.stopTime = stopTime;
    }
    public java.sql.Timestamp getStopTime() {
        return this.stopTime;
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

    public java.util.List<String> jobsUuid;
    public void setJobsUuid(java.util.List<String> jobsUuid) {
        this.jobsUuid = jobsUuid;
    }
    public java.util.List<String> getJobsUuid() {
        return this.jobsUuid;
    }

}
