package org.zstack.physicalserver;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdatePhysicalServerResourceAssignmentEvent extends APIEvent {
    private PhysicalServerResourceAssignmentInventory inventory;

    public APIUpdatePhysicalServerResourceAssignmentEvent() {
    }

    public APIUpdatePhysicalServerResourceAssignmentEvent(String apiId) {
        super(apiId);
    }

    public PhysicalServerResourceAssignmentInventory getInventory() {
        return inventory;
    }

    public void setInventory(PhysicalServerResourceAssignmentInventory inventory) {
        this.inventory = inventory;
    }
}
