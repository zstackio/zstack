package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 * Created by frank on 6/15/2015.
 */
public class APIGetFreeIpReply extends APIReply {
    private List<FreeIpInventory> inventories;

    public List<FreeIpInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<FreeIpInventory> inventories) {
        this.inventories = inventories;
    }
}
