package org.zstack.network.service.vip;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class APIQueryVipReply extends APIQueryReply {
    private List<VipInventory> inventories;

    public List<VipInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VipInventory> inventories) {
        this.inventories = inventories;
    }
}
