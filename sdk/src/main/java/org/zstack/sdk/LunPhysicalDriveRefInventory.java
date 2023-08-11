package org.zstack.sdk;



public class LunPhysicalDriveRefInventory  {

    public java.lang.String lunUuid;
    public void setLunUuid(java.lang.String lunUuid) {
        this.lunUuid = lunUuid;
    }
    public java.lang.String getLunUuid() {
        return this.lunUuid;
    }

    public java.lang.String physicalDriveUuid;
    public void setPhysicalDriveUuid(java.lang.String physicalDriveUuid) {
        this.physicalDriveUuid = physicalDriveUuid;
    }
    public java.lang.String getPhysicalDriveUuid() {
        return this.physicalDriveUuid;
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
