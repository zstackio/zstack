package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreatePhysicalServerEvent extends APIEvent {
    private PhysicalServerInventory inventory;

    public APICreatePhysicalServerEvent() {
        super(null);
    }

    public APICreatePhysicalServerEvent(String apiId) {
        super(apiId);
    }

    public PhysicalServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(PhysicalServerInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreatePhysicalServerEvent __example__() {
        APICreatePhysicalServerEvent event = new APICreatePhysicalServerEvent();
        PhysicalServerInventory inv = new PhysicalServerInventory();
        inv.setUuid(uuid());
        inv.setName("server1");
        inv.setZoneUuid(uuid());
        inv.setPoolUuid(uuid());
        inv.setManagementIp("192.168.1.100");
        inv.setArchitecture("x86_64");
        inv.setSerialNumber("SN123456");
        inv.setManufacturer("Dell");
        inv.setModel("PowerEdge R750");
        inv.setState("Enabled");
        inv.setPowerStatus("POWER_ON");
        inv.setOobManagementType("IPMI");
        inv.setOobAddress("192.168.1.200");
        inv.setOobPort(623);
        inv.setOobUsername("admin");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
