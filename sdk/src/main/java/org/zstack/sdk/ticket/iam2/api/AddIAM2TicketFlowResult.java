package org.zstack.sdk.ticket.iam2.api;

import org.zstack.sdk.ticket.entity.TicketFlowInventory;

public class AddIAM2TicketFlowResult {
    public TicketFlowInventory inventory;
    public void setInventory(TicketFlowInventory inventory) {
        this.inventory = inventory;
    }
    public TicketFlowInventory getInventory() {
        return this.inventory;
    }

}
