package org.zstack.sdk;



public class AgentVersionInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String agentType;
    public void setAgentType(java.lang.String agentType) {
        this.agentType = agentType;
    }
    public java.lang.String getAgentType() {
        return this.agentType;
    }

    public java.lang.String currentVersion;
    public void setCurrentVersion(java.lang.String currentVersion) {
        this.currentVersion = currentVersion;
    }
    public java.lang.String getCurrentVersion() {
        return this.currentVersion;
    }

    public java.lang.String expectVersion;
    public void setExpectVersion(java.lang.String expectVersion) {
        this.expectVersion = expectVersion;
    }
    public java.lang.String getExpectVersion() {
        return this.expectVersion;
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
