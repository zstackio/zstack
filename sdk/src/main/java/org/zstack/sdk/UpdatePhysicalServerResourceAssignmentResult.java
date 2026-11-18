package org.zstack.sdk;

import org.zstack.sdk.PhysicalServerResourceAssignmentInventory;

public class UpdatePhysicalServerResourceAssignmentResult {
    public PhysicalServerResourceAssignmentInventory inventory;
    public void setInventory(PhysicalServerResourceAssignmentInventory inventory) {
        this.inventory = inventory;
    }
    public PhysicalServerResourceAssignmentInventory getInventory() {
        return this.inventory;
    }

}
