package org.zstack.network.service.portforwarding;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryPortForwardingRuleReply extends APIQueryReply {
    private List<PortForwardingRuleInventory> inventories;

    public List<PortForwardingRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PortForwardingRuleInventory> inventories) {
        this.inventories = inventories;
    }
}
