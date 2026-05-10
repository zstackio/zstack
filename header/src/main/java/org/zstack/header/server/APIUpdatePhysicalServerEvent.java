package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIUpdatePhysicalServerEvent extends APIEvent {
    private PhysicalServerInventory inventory;

    public APIUpdatePhysicalServerEvent() {
        super(null);
    }

    public APIUpdatePhysicalServerEvent(String apiId) {
        super(apiId);
    }

    public PhysicalServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(PhysicalServerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdatePhysicalServerEvent __example__() {
        APIUpdatePhysicalServerEvent event = new APIUpdatePhysicalServerEvent();
        PhysicalServerInventory inv = new PhysicalServerInventory();
        inv.setUuid(uuid());
        inv.setName("server1-updated");
        inv.setZoneUuid(uuid());
        inv.setPoolUuid(uuid());
        inv.setManagementIp("192.168.1.101");
        inv.setArchitecture("x86_64");
        inv.setState("Enabled");
        inv.setPowerStatus("POWER_ON");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
