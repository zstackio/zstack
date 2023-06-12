package org.zstack.sdk;



public class ExternalManagementNodeBackupStorageRefInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String externalManagementNodeUuid;
    public void setExternalManagementNodeUuid(java.lang.String externalManagementNodeUuid) {
        this.externalManagementNodeUuid = externalManagementNodeUuid;
    }
    public java.lang.String getExternalManagementNodeUuid() {
        return this.externalManagementNodeUuid;
    }

    public java.lang.String backupStorageUuid;
    public void setBackupStorageUuid(java.lang.String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
    public java.lang.String getBackupStorageUuid() {
        return this.backupStorageUuid;
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
