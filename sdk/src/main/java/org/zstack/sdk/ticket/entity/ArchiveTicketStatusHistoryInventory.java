package org.zstack.sdk.ticket.entity;

import org.zstack.sdk.ticket.entity.TicketStatus;
import org.zstack.sdk.ticket.entity.TicketStatus;

public class ArchiveTicketStatusHistoryInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public int sequence;
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
    public int getSequence() {
        return this.sequence;
    }

    public java.lang.String ticketUuid;
    public void setTicketUuid(java.lang.String ticketUuid) {
        this.ticketUuid = ticketUuid;
    }
    public java.lang.String getTicketUuid() {
        return this.ticketUuid;
    }

    public java.lang.String historyUuid;
    public void setHistoryUuid(java.lang.String historyUuid) {
        this.historyUuid = historyUuid;
    }
    public java.lang.String getHistoryUuid() {
        return this.historyUuid;
    }

    public TicketStatus fromStatus;
    public void setFromStatus(TicketStatus fromStatus) {
        this.fromStatus = fromStatus;
    }
    public TicketStatus getFromStatus() {
        return this.fromStatus;
    }

    public TicketStatus toStatus;
    public void setToStatus(TicketStatus toStatus) {
        this.toStatus = toStatus;
    }
    public TicketStatus getToStatus() {
        return this.toStatus;
    }

    public java.lang.String comment;
    public void setComment(java.lang.String comment) {
        this.comment = comment;
    }
    public java.lang.String getComment() {
        return this.comment;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    public java.lang.String operationContextType;
    public void setOperationContextType(java.lang.String operationContextType) {
        this.operationContextType = operationContextType;
    }
    public java.lang.String getOperationContextType() {
        return this.operationContextType;
    }

    public java.util.LinkedHashMap operationContext;
    public void setOperationContext(java.util.LinkedHashMap operationContext) {
        this.operationContext = operationContext;
    }
    public java.util.LinkedHashMap getOperationContext() {
        return this.operationContext;
    }

    public java.lang.String operatorType;
    public void setOperatorType(java.lang.String operatorType) {
        this.operatorType = operatorType;
    }
    public java.lang.String getOperatorType() {
        return this.operatorType;
    }

    public java.lang.String operatorUuid;
    public void setOperatorUuid(java.lang.String operatorUuid) {
        this.operatorUuid = operatorUuid;
    }
    public java.lang.String getOperatorUuid() {
        return this.operatorUuid;
    }

    public java.lang.String flowName;
    public void setFlowName(java.lang.String flowName) {
        this.flowName = flowName;
    }
    public java.lang.String getFlowName() {
        return flowName;
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
