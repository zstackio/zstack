package org.zstack.sdk;

import org.zstack.sdk.RuleAttributeType;
import org.zstack.sdk.AttributePurpose;

public class SSOClientAttributeInventory  {

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

    public java.lang.String value;
    public void setValue(java.lang.String value) {
        this.value = value;
    }
    public java.lang.String getValue() {
        return this.value;
    }

    public RuleAttributeType type;
    public void setType(RuleAttributeType type) {
        this.type = type;
    }
    public RuleAttributeType getType() {
        return this.type;
    }

    public AttributePurpose purpose;
    public void setPurpose(AttributePurpose purpose) {
        this.purpose = purpose;
    }
    public AttributePurpose getPurpose() {
        return this.purpose;
    }

    public java.lang.String ssoClientUuid;
    public void setSsoClientUuid(java.lang.String ssoClientUuid) {
        this.ssoClientUuid = ssoClientUuid;
    }
    public java.lang.String getSsoClientUuid() {
        return this.ssoClientUuid;
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
