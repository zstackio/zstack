package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2021/3/16.
 */
public class CreateRootVolumeTemplateFromVolumeSnapshotReply extends MessageReply {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
