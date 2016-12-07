package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 7/19/2015.
 */
@RestResponse(allTo = "inventories")
public class APIGetVmAttachableL3NetworkReply extends APIReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }
}
