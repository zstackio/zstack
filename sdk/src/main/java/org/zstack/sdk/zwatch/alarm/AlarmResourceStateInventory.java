package org.zstack.sdk.zwatch.alarm;

import org.zstack.sdk.zwatch.alarm.AlarmStatus;

public class AlarmResourceStateInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String alarmUuid;
    public void setAlarmUuid(java.lang.String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
    public java.lang.String getAlarmUuid() {
        return this.alarmUuid;
    }

    public java.lang.String identifyLabel;
    public void setIdentifyLabel(java.lang.String identifyLabel) {
        this.identifyLabel = identifyLabel;
    }
    public java.lang.String getIdentifyLabel() {
        return this.identifyLabel;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public java.lang.String resourceType;
    public void setResourceType(java.lang.String resourceType) {
        this.resourceType = resourceType;
    }
    public java.lang.String getResourceType() {
        return this.resourceType;
    }

    public AlarmStatus status;
    public void setStatus(AlarmStatus status) {
        this.status = status;
    }
    public AlarmStatus getStatus() {
        return this.status;
    }

    public java.lang.Long lastStatusChangeTime;
    public void setLastStatusChangeTime(java.lang.Long lastStatusChangeTime) {
        this.lastStatusChangeTime = lastStatusChangeTime;
    }
    public java.lang.Long getLastStatusChangeTime() {
        return this.lastStatusChangeTime;
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
