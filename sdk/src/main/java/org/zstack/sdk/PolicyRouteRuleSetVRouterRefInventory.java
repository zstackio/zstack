package org.zstack.sdk;



public class PolicyRouteRuleSetVRouterRefInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String vRouterUuid;
    public void setVRouterUuid(java.lang.String vRouterUuid) {
        this.vRouterUuid = vRouterUuid;
    }
    public java.lang.String getVRouterUuid() {
        return this.vRouterUuid;
    }

    public java.lang.String ruleSetUuid;
    public void setRuleSetUuid(java.lang.String ruleSetUuid) {
        this.ruleSetUuid = ruleSetUuid;
    }
    public java.lang.String getRuleSetUuid() {
        return this.ruleSetUuid;
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
