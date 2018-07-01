package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

/**
 * Create by weiwang at 2018/6/19
 */
public class CreateDataVolumeFromVolumeTemplateReply extends MessageReply {
    private VolumeInventory inventory;

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
