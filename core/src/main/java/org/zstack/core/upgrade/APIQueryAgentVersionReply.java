package org.zstack.core.upgrade;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryAgentVersionReply extends APIQueryReply {
    private List<AgentVersionInventory> inventories;

    public List<AgentVersionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AgentVersionInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAgentVersionReply __example__() {
        APIQueryAgentVersionReply reply = new APIQueryAgentVersionReply();

        return reply;
    }
}
