package org.zstack.sdk;

import org.zstack.sdk.ClusterDpmInventory;

public class UpdateClusterDpmResult {
    public ClusterDpmInventory inventory;
    public void setInventory(ClusterDpmInventory inventory) {
        this.inventory = inventory;
    }
    public ClusterDpmInventory getInventory() {
        return this.inventory;
    }

}
