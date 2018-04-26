package org.zstack.header.identity.role.api;

import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateRoleEvent extends APIEvent {
    private RoleInventory inventory;

    public APICreateRoleEvent() {
    }

    public APICreateRoleEvent(String apiId) {
        super(apiId);
    }

    public RoleInventory getInventory() {
        return inventory;
    }

    public void setInventory(RoleInventory inventory) {
        this.inventory = inventory;
    }
}
