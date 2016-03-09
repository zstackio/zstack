package org.zstack.core.config;

import org.zstack.header.message.APIReply;

public class APIGetGlobalConfigReply extends APIReply {
    private GlobalConfigInventory inventory;

    public GlobalConfigInventory getInventory() {
        return inventory;
    }

    public void setInventory(GlobalConfigInventory inventory) {
        this.inventory = inventory;
    }
}
