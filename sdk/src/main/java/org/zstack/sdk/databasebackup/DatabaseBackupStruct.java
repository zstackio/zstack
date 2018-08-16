package org.zstack.sdk.databasebackup;



public class DatabaseBackupStruct  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public java.lang.String installPath;
    public void setInstallPath(java.lang.String installPath) {
        this.installPath = installPath;
    }
    public java.lang.String getInstallPath() {
        return this.installPath;
    }

    public java.sql.Timestamp createdTime;
    public void setCreatedTime(java.sql.Timestamp createdTime) {
        this.createdTime = createdTime;
    }
    public java.sql.Timestamp getCreatedTime() {
        return this.createdTime;
    }

}
