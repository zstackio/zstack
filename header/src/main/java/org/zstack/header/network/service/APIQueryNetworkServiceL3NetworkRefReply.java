package org.zstack.header.network.service;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryNetworkServiceL3NetworkRefReply extends APIQueryReply {
    private List<NetworkServiceL3NetworkRefInventory> inventories;

    public List<NetworkServiceL3NetworkRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkServiceL3NetworkRefInventory> inventories) {
        this.inventories = inventories;
    }
    
    
}
