package org.zstack.sdk.ticket.api;

import org.zstack.sdk.ticket.entity.TicketFlowCollectionInventory;

public class CreateTickFlowCollectionResult {
    public TicketFlowCollectionInventory inventory;
    public void setInventory(TicketFlowCollectionInventory inventory) {
        this.inventory = inventory;
    }
    public TicketFlowCollectionInventory getInventory() {
        return this.inventory;
    }

}
