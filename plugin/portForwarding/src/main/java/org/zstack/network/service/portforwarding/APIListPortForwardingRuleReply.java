package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListPortForwardingRuleReply extends APIReply {
    private List<PortForwardingRuleInventory> inventories;

    public List<PortForwardingRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PortForwardingRuleInventory> inventories) {
        this.inventories = inventories;
    }
}
