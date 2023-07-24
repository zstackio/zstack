package org.zstack.sdk;

import org.zstack.sdk.XmlHookType;

public class XmlHookInventory  {

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

    public XmlHookType type;
    public void setType(XmlHookType type) {
        this.type = type;
    }
    public XmlHookType getType() {
        return this.type;
    }

    public java.lang.String hookScript;
    public void setHookScript(java.lang.String hookScript) {
        this.hookScript = hookScript;
    }
    public java.lang.String getHookScript() {
        return this.hookScript;
    }

    public java.lang.String libvirtVersion;
    public void setLibvirtVersion(java.lang.String libvirtVersion) {
        this.libvirtVersion = libvirtVersion;
    }
    public java.lang.String getLibvirtVersion() {
        return this.libvirtVersion;
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
