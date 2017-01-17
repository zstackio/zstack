package org.zstack.header.configuration;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryDiskOfferingReply extends APIQueryReply {
    private List<DiskOfferingInventory> inventories;

    public List<DiskOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<DiskOfferingInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryDiskOfferingReply __example__() {
        APIQueryDiskOfferingReply reply = new APIQueryDiskOfferingReply();


        return reply;
    }

}
