package org.zstack.header.network.l3;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryIpRangeReply extends APIQueryReply {
    private List<IpRangeInventory> inventories;

    public List<IpRangeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IpRangeInventory> inventories) {
        this.inventories = inventories;
    }
}
