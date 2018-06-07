package org.zstack.sdk.iam2.entity;

import org.zstack.sdk.iam2.entity.AttributeType;

public class IAM2AttributeInventory  {

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

    public AttributeType type;
    public void setType(AttributeType type) {
        this.type = type;
    }
    public AttributeType getType() {
        return this.type;
    }

}
