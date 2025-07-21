package org.zstack.sdnController.header;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by boce.wang on 06/13/2025.
 */
public class PullSdnControllerTenantReply extends MessageReply {
    private List<H3cSdnControllerTenantInventory> inventories;

    public List<H3cSdnControllerTenantInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<H3cSdnControllerTenantInventory> inventories) {
        this.inventories = inventories;
    }
}
