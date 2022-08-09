package org.zstack.sdk.ticket.entity;

import org.zstack.sdk.ticket.entity.TicketStatus;

public class TicketInventory  {

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

    public TicketStatus status;
    public void setStatus(TicketStatus status) {
        this.status = status;
    }
    public TicketStatus getStatus() {
        return this.status;
    }

    public java.util.List request;
    public void setRequest(java.util.List request) {
        this.request = request;
    }
    public java.util.List getRequest() {
        return this.request;
    }

    public java.lang.String accountSystemType;
    public void setAccountSystemType(java.lang.String accountSystemType) {
        this.accountSystemType = accountSystemType;
    }
    public java.lang.String getAccountSystemType() {
        return this.accountSystemType;
    }

    public java.lang.String ticketTypeUuid;
    public void setTicketTypeUuid(java.lang.String ticketTypeUuid) {
        this.ticketTypeUuid = ticketTypeUuid;
    }
    public java.lang.String getTicketTypeUuid() {
        return this.ticketTypeUuid;
    }

    public java.lang.String organizationUuid;
    public void setOrganizationUuid(java.lang.String organizationUuid) {
        this.organizationUuid = organizationUuid;
    }
    public java.lang.String getOrganizationUuid() {
        return this.organizationUuid;
    }

    public java.lang.Object accountSystemContext;
    public void setAccountSystemContext(java.lang.Object accountSystemContext) {
        this.accountSystemContext = accountSystemContext;
    }
    public java.lang.Object getAccountSystemContext() {
        return this.accountSystemContext;
    }

    public java.lang.String currentFlowUuid;
    public void setCurrentFlowUuid(java.lang.String currentFlowUuid) {
        this.currentFlowUuid = currentFlowUuid;
    }
    public java.lang.String getCurrentFlowUuid() {
        return this.currentFlowUuid;
    }

    public java.lang.String flowCollectionUuid;
    public void setFlowCollectionUuid(java.lang.String flowCollectionUuid) {
        this.flowCollectionUuid = flowCollectionUuid;
    }
    public java.lang.String getFlowCollectionUuid() {
        return this.flowCollectionUuid;
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
