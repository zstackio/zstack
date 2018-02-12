package org.zstack.sdk;

import org.zstack.sdk.OssBucketInventory;

public class AddOssBucketFromRemoteResult {
    public OssBucketInventory inventory;
    public void setInventory(OssBucketInventory inventory) {
        this.inventory = inventory;
    }
    public OssBucketInventory getInventory() {
        return this.inventory;
    }

}
