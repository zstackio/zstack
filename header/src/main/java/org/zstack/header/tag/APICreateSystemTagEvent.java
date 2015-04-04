package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;

/**
 */
public class APICreateSystemTagEvent extends APIEvent {
    private SystemTagInventory inventory;

    public APICreateSystemTagEvent(String apiId) {
        super(apiId);
    }

    public APICreateSystemTagEvent() {
        super(null);
    }

    public SystemTagInventory getInventory() {
        return inventory;
    }

    public void setInventory(SystemTagInventory inventory) {
        this.inventory = inventory;
    }
}
