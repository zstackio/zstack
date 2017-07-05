package org.zstack.header.zone;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryZoneReply extends APIQueryReply {
    private List<ZoneInventory> inventories;

    public List<ZoneInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ZoneInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryZoneReply __example__() {
        APIQueryZoneReply reply = new APIQueryZoneReply();
        ZoneInventory zone = new ZoneInventory();
        zone.setName("TestZone");
        zone.setUuid(uuid());
        zone.setDescription("Test");
        zone.setState(ZoneState.Enabled.toString());
        zone.setType("zstack");
        zone.setCreateDate(new Timestamp(System.currentTimeMillis()));
        zone.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setSuccess(true);
        reply.setInventories(asList(zone));
        return reply;
    }
}
