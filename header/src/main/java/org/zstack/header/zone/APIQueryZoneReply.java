package org.zstack.header.zone;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryZoneReply extends APIQueryReply {
    private List<ZoneInventory> inventories;

    public static APIQueryZoneReply __example__() {
        APIQueryZoneReply reply = new APIQueryZoneReply();

        ZoneInventory inv = new ZoneInventory();
        inv.setName("zone1");
        inv.setDescription("this is a zone");
        inv.setState(ZoneState.Enabled.toString());
        inv.setUuid(uuid());

        reply.inventories = asList(inv);

        return reply;
    }

    public List<ZoneInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ZoneInventory> inventories) {
        this.inventories = inventories;
    }
}
