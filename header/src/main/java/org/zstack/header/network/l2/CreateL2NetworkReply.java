package org.zstack.header.network.l2;

import org.zstack.header.message.MessageReply;

public class CreateL2NetworkReply extends MessageReply {
    private L2NetworkInventory inventory;

    public L2NetworkInventory getInventory() { return inventory; }
    public void setInventory(L2NetworkInventory value) { inventory = value; }
}
