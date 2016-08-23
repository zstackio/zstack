package org.zstack.header.vm;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 * Created by xing5 on 2016/8/23.
 */
public class APIGetInterdependentL3NetworkImageReply extends APIReply {
    private List inventories;

    public List getInventories() {
        return inventories;
    }

    public void setInventories(List inventories) {
        this.inventories = inventories;
    }
}
