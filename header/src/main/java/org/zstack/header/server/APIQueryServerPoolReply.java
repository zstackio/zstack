package org.zstack.header.server;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryServerPoolReply extends APIQueryReply {
    private List<ServerPoolInventory> inventories;

    public List<ServerPoolInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ServerPoolInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryServerPoolReply __example__() {
        APIQueryServerPoolReply reply = new APIQueryServerPoolReply();
        ServerPoolInventory inv = new ServerPoolInventory();
        inv.setUuid(uuid());
        inv.setName("pool-rack-A1");
        inv.setState("Enabled");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(inv));
        return reply;
    }
}
