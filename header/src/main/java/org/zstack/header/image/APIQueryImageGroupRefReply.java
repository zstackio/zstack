package org.zstack.header.image;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryImageGroupRefReply  extends APIQueryReply {
    private List<ImageGroupRefInventory> inventories;

    public List<ImageGroupRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageGroupRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryImageGroupRefReply __example__() {
        APIQueryImageGroupRefReply reply = new APIQueryImageGroupRefReply();


        return reply;
    }

}
