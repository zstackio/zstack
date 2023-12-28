package org.zstack.network.service.lb;

import org.zstack.header.message.MessageReply;

public class DetachVipFromLoadBalancerReply extends MessageReply {
    private LoadBalancerInventory inventory;

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
}
