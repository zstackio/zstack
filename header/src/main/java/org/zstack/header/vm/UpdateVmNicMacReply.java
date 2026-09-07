package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

public class UpdateVmNicMacReply extends MessageReply {
    private VmNicInventory inventory;

    public VmNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmNicInventory inventory) {
        this.inventory = inventory;
    }
}
