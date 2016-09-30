package org.zstack.header.vm;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 * Created by xing5 on 2016/9/20.
 */
public class APIGetCandidateVmForAttachingIsoReply extends APIReply {
    private List<VmInstanceInventory> inventories;

    public List<VmInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceInventory> inventories) {
        this.inventories = inventories;
    }
}
