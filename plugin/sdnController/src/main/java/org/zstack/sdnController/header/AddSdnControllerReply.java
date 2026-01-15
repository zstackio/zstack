package org.zstack.sdnController.header;

import org.zstack.header.message.MessageReply;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;

public class AddSdnControllerReply extends MessageReply {
    private SdnControllerInventory inventory;

    public SdnControllerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SdnControllerInventory inventory) {
        this.inventory = inventory;
    }
}
