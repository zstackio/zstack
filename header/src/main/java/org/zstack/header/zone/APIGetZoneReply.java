package org.zstack.header.zone;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIGetZoneReply extends APIReply {
    private List<ZoneInventory> inventories;

    public List<ZoneInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ZoneInventory> inventories) {
        this.inventories = inventories;
    }
}
