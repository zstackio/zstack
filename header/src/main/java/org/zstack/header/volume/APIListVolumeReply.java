package org.zstack.header.volume;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListVolumeReply extends APIReply {
    private List<VolumeInventory> inventories;

    public List<VolumeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListVolumeReply __example__() {
        APIListVolumeReply reply = new APIListVolumeReply();


        return reply;
    }

}
