package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIAttachPhysicalServerRoleEvent extends APIEvent {
    private PhysicalServerRoleInventory inventory;

    public APIAttachPhysicalServerRoleEvent() {
        super(null);
    }

    public APIAttachPhysicalServerRoleEvent(String apiId) {
        super(apiId);
    }

    public PhysicalServerRoleInventory getInventory() {
        return inventory;
    }

    public void setInventory(PhysicalServerRoleInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAttachPhysicalServerRoleEvent __example__() {
        APIAttachPhysicalServerRoleEvent event = new APIAttachPhysicalServerRoleEvent();
        PhysicalServerRoleInventory inv = new PhysicalServerRoleInventory();
        inv.setUuid(uuid());
        inv.setServerUuid(uuid());
        inv.setRoleType("KVM_HOST");
        inv.setRoleUuid(uuid());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
