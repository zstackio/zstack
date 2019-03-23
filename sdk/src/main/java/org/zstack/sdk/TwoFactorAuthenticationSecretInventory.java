package org.zstack.sdk;

import org.zstack.sdk.TwoFactorAuthenticationSecretStatus;

public class TwoFactorAuthenticationSecretInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String secret;
    public void setSecret(java.lang.String secret) {
        this.secret = secret;
    }
    public java.lang.String getSecret() {
        return this.secret;
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

    public TwoFactorAuthenticationSecretStatus status;
    public void setStatus(TwoFactorAuthenticationSecretStatus status) {
        this.status = status;
    }
    public TwoFactorAuthenticationSecretStatus getStatus() {
        return this.status;
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

    public java.lang.String userUuid;
    public void setUserUuid(java.lang.String userUuid) {
        this.userUuid = userUuid;
    }
    public java.lang.String getUserUuid() {
        return this.userUuid;
    }

    public java.lang.String userType;
    public void setUserType(java.lang.String userType) {
        this.userType = userType;
    }
    public java.lang.String getUserType() {
        return this.userType;
    }

}
