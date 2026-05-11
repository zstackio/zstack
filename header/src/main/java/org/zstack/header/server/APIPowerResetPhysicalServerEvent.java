package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIPowerResetPhysicalServerEvent extends APIEvent {
    private PhysicalServerInventory inventory;

    public APIPowerResetPhysicalServerEvent() {
        super(null);
    }

    public APIPowerResetPhysicalServerEvent(String apiId) {
        super(apiId);
    }

    public PhysicalServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(PhysicalServerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIPowerResetPhysicalServerEvent __example__() {
        APIPowerResetPhysicalServerEvent event = new APIPowerResetPhysicalServerEvent();
        PhysicalServerInventory inv = new PhysicalServerInventory();
        inv.setUuid(uuid());
        inv.setName("server1");
        inv.setZoneUuid(uuid());
        inv.setPoolUuid(uuid());
        inv.setManagementIp("192.168.1.100");
        inv.setArchitecture("x86_64");
        inv.setState("Enabled");
        inv.setPowerStatus("POWER_ON");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
