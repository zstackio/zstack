package org.zstack.sdk;

import org.zstack.sdk.CdpPolicyState;

public class CdpPolicyInventory  {

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

    public CdpPolicyState state;
    public void setState(CdpPolicyState state) {
        this.state = state;
    }
    public CdpPolicyState getState() {
        return this.state;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.Integer retentionTimePerDay;
    public void setRetentionTimePerDay(java.lang.Integer retentionTimePerDay) {
        this.retentionTimePerDay = retentionTimePerDay;
    }
    public java.lang.Integer getRetentionTimePerDay() {
        return this.retentionTimePerDay;
    }

    public java.lang.Integer hourlyRpSinceDay;
    public void setHourlyRpSinceDay(java.lang.Integer hourlyRpSinceDay) {
        this.hourlyRpSinceDay = hourlyRpSinceDay;
    }
    public java.lang.Integer getHourlyRpSinceDay() {
        return this.hourlyRpSinceDay;
    }

    public java.lang.Integer dailyRpSinceDay;
    public void setDailyRpSinceDay(java.lang.Integer dailyRpSinceDay) {
        this.dailyRpSinceDay = dailyRpSinceDay;
    }
    public java.lang.Integer getDailyRpSinceDay() {
        return this.dailyRpSinceDay;
    }

    public java.lang.Integer expireTimeInDay;
    public void setExpireTimeInDay(java.lang.Integer expireTimeInDay) {
        this.expireTimeInDay = expireTimeInDay;
    }
    public java.lang.Integer getExpireTimeInDay() {
        return this.expireTimeInDay;
    }

    public java.lang.Integer fullBackupIntervalInDay;
    public void setFullBackupIntervalInDay(java.lang.Integer fullBackupIntervalInDay) {
        this.fullBackupIntervalInDay = fullBackupIntervalInDay;
    }
    public java.lang.Integer getFullBackupIntervalInDay() {
        return this.fullBackupIntervalInDay;
    }

    public java.lang.Integer recoveryPointPerSecond;
    public void setRecoveryPointPerSecond(java.lang.Integer recoveryPointPerSecond) {
        this.recoveryPointPerSecond = recoveryPointPerSecond;
    }
    public java.lang.Integer getRecoveryPointPerSecond() {
        return this.recoveryPointPerSecond;
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
