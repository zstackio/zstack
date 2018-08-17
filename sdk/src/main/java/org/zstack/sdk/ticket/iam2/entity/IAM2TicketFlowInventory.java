package org.zstack.sdk.ticket.iam2.entity;



public class IAM2TicketFlowInventory extends org.zstack.sdk.ticket.entity.TicketFlowInventory {

    public java.lang.String approverUuid;
    public void setApproverUuid(java.lang.String approverUuid) {
        this.approverUuid = approverUuid;
    }
    public java.lang.String getApproverUuid() {
        return this.approverUuid;
    }

    public boolean valid;
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    public boolean getValid() {
        return this.valid;
    }

}
