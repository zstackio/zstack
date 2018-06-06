package org.zstack.sdk.ticket.entity;



public class TicketFlowInventory  {

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

    public java.lang.String parentFlowUuid;
    public void setParentFlowUuid(java.lang.String parentFlowUuid) {
        this.parentFlowUuid = parentFlowUuid;
    }
    public java.lang.String getParentFlowUuid() {
        return this.parentFlowUuid;
    }

    public java.lang.String flowContext;
    public void setFlowContext(java.lang.String flowContext) {
        this.flowContext = flowContext;
    }
    public java.lang.String getFlowContext() {
        return this.flowContext;
    }

    public java.lang.String flowContextType;
    public void setFlowContextType(java.lang.String flowContextType) {
        this.flowContextType = flowContextType;
    }
    public java.lang.String getFlowContextType() {
        return this.flowContextType;
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
