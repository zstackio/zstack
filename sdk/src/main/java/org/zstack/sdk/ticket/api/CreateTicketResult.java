package org.zstack.sdk.ticket.api;

import org.zstack.sdk.ticket.entity.TicketInventory;

public class CreateTicketResult {
    public TicketInventory inventory;
    public void setInventory(TicketInventory inventory) {
        this.inventory = inventory;
    }
    public TicketInventory getInventory() {
        return this.inventory;
    }

}
