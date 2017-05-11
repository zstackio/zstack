package org.zstack.header.image;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListImageReply extends APIReply {
    private List<ImageInventory> inventories;

    public List<ImageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageInventory> inventories) {
        this.inventories = inventories;
    }


    public static APIListImageReply __example__() {
        APIListImageReply msg = new APIListImageReply();
        return msg;
    }
    
}