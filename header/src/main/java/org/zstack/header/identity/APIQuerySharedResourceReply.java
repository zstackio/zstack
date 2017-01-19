package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 2/23/2016.
 */
@RestResponse(allTo = "inventories")
public class APIQuerySharedResourceReply extends APIQueryReply {
    private List<SharedResourceInventory> inventories;

    public List<SharedResourceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SharedResourceInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySharedResourceReply __example__() {
        APIQuerySharedResourceReply reply = new APIQuerySharedResourceReply();
        SharedResourceInventory inventory = new SharedResourceInventory();
        inventory.setOwnerAccountUuid(uuid());
        inventory.setReceiverAccountUuid(uuid());
        inventory.setResourceUuid(uuid());
        inventory.setResourceType("ImageVO");
        inventory.setToPublic(false);
        reply.setInventories(list(inventory));
        return reply;
    }

}
