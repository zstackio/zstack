package org.zstack.header.server;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryProvisionNetworkReply extends APIQueryReply {
    private List<PhysicalServerProvisionNetworkInventory> inventories;

    public List<PhysicalServerProvisionNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PhysicalServerProvisionNetworkInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryProvisionNetworkReply __example__() {
        APIQueryProvisionNetworkReply reply = new APIQueryProvisionNetworkReply();
        PhysicalServerProvisionNetworkInventory inv = new PhysicalServerProvisionNetworkInventory();
        inv.setUuid(uuid());
        inv.setName("provision-net-1");
        inv.setType("STANDALONE_PXE");
        reply.setInventories(asList(inv));
        return reply;
    }
}
