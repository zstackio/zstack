package org.zstack.header.zone;

import org.zstack.header.message.APIReply;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

public class APIGetZoneReply extends APIReply {
    private List<ZoneInventory> inventories;

    public List<ZoneInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ZoneInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetZoneReply __example__() {
        APIGetZoneReply reply = new APIGetZoneReply();
        ZoneInventory zone = new ZoneInventory();
        zone.setName("TestZone");
        zone.setUuid(uuid());
        zone.setDescription("Test");
        zone.setState("Enabled");
        zone.setType("zstack");
        zone.setCreateDate(new Timestamp(System.currentTimeMillis()));
        zone.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setInventories(asList(zone));
        reply.setSuccess(true);
        return reply;
    }

}
