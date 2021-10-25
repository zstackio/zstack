package org.zstack.sdk;

public class CCSCertificateUserRefInventory {

    public java.lang.String userUuid;
    public void setUserUuid(java.lang.String userUuid) {
        this.userUuid = userUuid;
    }
    public java.lang.String getUserUuid() {
        return this.userUuid;
    }

    public java.lang.String certificateUuid;
    public void setCertificateUuid(java.lang.String certificateUuid) {
        this.certificateUuid = certificateUuid;
    }
    public java.lang.String getCertificateUuid() {
        return this.certificateUuid;
    }

    public CCSCertificateUserState state;
    public void setState(CCSCertificateUserState state) {
        this.state = state;
    }
    public CCSCertificateUserState getState() {
        return this.state;
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
