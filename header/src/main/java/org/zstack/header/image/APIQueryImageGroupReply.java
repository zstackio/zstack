package org.zstack.header.image;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryImageGroupReply extends APIQueryReply {
    private List<ImageGroupInventory> inventories;

    public List<ImageGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryImageGroupReply __example__() {
        APIQueryImageGroupReply reply = new APIQueryImageGroupReply();


        return reply;
    }

}
