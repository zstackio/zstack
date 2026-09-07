package org.zstack.header.network.l3;

import org.zstack.header.message.MessageReply;

public class CreateL3NetworkReply extends MessageReply {
    private L3NetworkInventory inventory;

    public L3NetworkInventory getInventory() { return inventory; }
    public void setInventory(L3NetworkInventory value) { inventory = value; }
}
