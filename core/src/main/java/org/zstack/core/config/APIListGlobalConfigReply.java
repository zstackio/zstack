package org.zstack.core.config;

import org.zstack.header.message.APIReply;

public class APIListGlobalConfigReply extends APIReply {
	private GlobalConfigInventory[] inventories;

	public GlobalConfigInventory[] getInventories() {
    	return inventories;
    }

	public void setInventories(GlobalConfigInventory[] inventories) {
    	this.inventories = inventories;
    }

    public static APIListGlobalConfigReply __example__() {
        APIListGlobalConfigReply reply = new APIListGlobalConfigReply();


        return reply;
    }

}
