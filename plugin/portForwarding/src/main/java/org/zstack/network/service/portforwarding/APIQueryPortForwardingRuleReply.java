package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryPortForwardingRuleReply extends APIQueryReply {
    private List<PortForwardingRuleInventory> inventories;

    public List<PortForwardingRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PortForwardingRuleInventory> inventories) {
        this.inventories = inventories;
    }
}
