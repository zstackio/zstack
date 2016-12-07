package org.zstack.header.vm;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by xing5 on 2016/9/21.
 */
@RestResponse(allTo = "inventories")
public class APIGetCandidateIsoForAttachingVmReply extends APIReply {
    private List<ImageInventory> inventories;

    public List<ImageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageInventory> inventories) {
        this.inventories = inventories;
    }
}
