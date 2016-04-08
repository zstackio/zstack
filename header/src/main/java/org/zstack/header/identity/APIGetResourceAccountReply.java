package org.zstack.header.identity;

import org.zstack.header.message.APIReply;

import java.util.Map;

/**
 * Created by xing5 on 2016/4/8.
 */
public class APIGetResourceAccountReply extends APIReply {
    private Map<String, AccountInventory> inventories;

    public Map<String, AccountInventory> getInventories() {
        return inventories;
    }

    public void setInventories(Map<String, AccountInventory> inventories) {
        this.inventories = inventories;
    }
}
