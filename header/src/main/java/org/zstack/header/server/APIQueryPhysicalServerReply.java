package org.zstack.header.server;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryPhysicalServerReply extends APIQueryReply {
    private List<PhysicalServerInventory> inventories;

    public List<PhysicalServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PhysicalServerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPhysicalServerReply __example__() {
        APIQueryPhysicalServerReply reply = new APIQueryPhysicalServerReply();
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
        reply.setSuccess(true);
        reply.setInventories(asList(inv));
        return reply;
    }
}
