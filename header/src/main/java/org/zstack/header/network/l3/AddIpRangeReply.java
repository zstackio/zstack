package org.zstack.header.network.l3;

import org.zstack.header.message.MessageReply;

public class AddIpRangeReply extends MessageReply {
    private IpRangeInventory inventory;

    public IpRangeInventory getInventory() { return inventory; }
    public void setInventory(IpRangeInventory value) { inventory = value; }
}
