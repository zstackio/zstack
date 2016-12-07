package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by xing5 on 2016/8/23.
 */
@RestResponse(allTo = "inventories")
public class APIGetInterdependentL3NetworkImageReply extends APIReply {
    private List inventories;

    public List getInventories() {
        return inventories;
    }

    public void setInventories(List inventories) {
        this.inventories = inventories;
    }
}
