package org.zstack.sdk.zmigrate.api;



public class GetZMigrateInfosResult {
    public java.lang.String zmigrateVmInstanceStatus;
    public void setZmigrateVmInstanceStatus(java.lang.String zmigrateVmInstanceStatus) {
        this.zmigrateVmInstanceStatus = zmigrateVmInstanceStatus;
    }
    public java.lang.String getZmigrateVmInstanceStatus() {
        return this.zmigrateVmInstanceStatus;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public long platforms;
    public void setPlatforms(long platforms) {
        this.platforms = platforms;
    }
    public long getPlatforms() {
        return this.platforms;
    }

    public long gateways;
    public void setGateways(long gateways) {
        this.gateways = gateways;
    }
    public long getGateways() {
        return this.gateways;
    }

    public long migrateJobs;
    public void setMigrateJobs(long migrateJobs) {
        this.migrateJobs = migrateJobs;
    }
    public long getMigrateJobs() {
        return this.migrateJobs;
    }

    public long zmigrateStartTime;
    public void setZmigrateStartTime(long zmigrateStartTime) {
        this.zmigrateStartTime = zmigrateStartTime;
    }
    public long getZmigrateStartTime() {
        return this.zmigrateStartTime;
    }

}
