package org.zstack.sdk;

import org.zstack.sdk.PhysicalServerRoleInventory;

public class AttachPhysicalServerRoleResult {
    public PhysicalServerRoleInventory inventory;
    public void setInventory(PhysicalServerRoleInventory inventory) {
        this.inventory = inventory;
    }
    public PhysicalServerRoleInventory getInventory() {
        return this.inventory;
    }

}
