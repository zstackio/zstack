package org.zstack.sdk;

import org.zstack.sdk.IpSetType;

public class VpcFirewallIpSetTemplateInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String sourceValue;
    public void setSourceValue(java.lang.String sourceValue) {
        this.sourceValue = sourceValue;
    }
    public java.lang.String getSourceValue() {
        return this.sourceValue;
    }

    public java.lang.String destValue;
    public void setDestValue(java.lang.String destValue) {
        this.destValue = destValue;
    }
    public java.lang.String getDestValue() {
        return this.destValue;
    }

    public IpSetType type;
    public void setType(IpSetType type) {
        this.type = type;
    }
    public IpSetType getType() {
        return this.type;
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

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

}
