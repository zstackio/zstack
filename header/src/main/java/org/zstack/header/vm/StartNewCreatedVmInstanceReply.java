package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

public class StartNewCreatedVmInstanceReply extends MessageReply {
    private VmInstanceInventory vmInventory;

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }
}
