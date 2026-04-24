package org.zstack.sdk;

import org.zstack.sdk.HostCacheStoreInventory;

public class UpdateHostCacheStoreResult {
    public HostCacheStoreInventory inventory;
    public void setInventory(HostCacheStoreInventory inventory) {
        this.inventory = inventory;
    }
    public HostCacheStoreInventory getInventory() {
        return this.inventory;
    }

}
