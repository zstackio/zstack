package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

public class UpdateVmOSReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
