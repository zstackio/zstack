package org.zstack.sdk;



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

    public java.lang.Integer incrementalPointPerMinute;
    public void setIncrementalPointPerMinute(java.lang.Integer incrementalPointPerMinute) {
        this.incrementalPointPerMinute = incrementalPointPerMinute;
    }
    public java.lang.Integer getIncrementalPointPerMinute() {
        return this.incrementalPointPerMinute;
    }

    public java.lang.Integer recoveryPointPerSecond;
    public void setRecoveryPointPerSecond(java.lang.Integer recoveryPointPerSecond) {
        this.recoveryPointPerSecond = recoveryPointPerSecond;
    }
    public java.lang.Integer getRecoveryPointPerSecond() {
        return this.recoveryPointPerSecond;
    }

}
