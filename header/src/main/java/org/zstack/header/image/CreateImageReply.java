package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

public abstract class CreateImageReply extends MessageReply {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
