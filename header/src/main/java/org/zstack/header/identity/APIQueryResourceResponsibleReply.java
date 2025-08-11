package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryResourceResponsibleReply extends APIQueryReply {
    private List<ResourceResponsibleInventory> inventories;

    public List<ResourceResponsibleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceResponsibleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryResourceResponsibleReply __example__() {
        APIQueryResourceResponsibleReply reply = new APIQueryResourceResponsibleReply();
        return reply;
    }
}
