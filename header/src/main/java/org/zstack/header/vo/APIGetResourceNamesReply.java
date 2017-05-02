package org.zstack.header.vo;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by xing5 on 2017/5/1.
 */
@RestResponse(allTo = "inventories")
public class APIGetResourceNamesReply extends APIReply {
    private List<ResourceInventory> inventories;

    public List<ResourceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceInventory> inventories) {
        this.inventories = inventories;
    }
}
