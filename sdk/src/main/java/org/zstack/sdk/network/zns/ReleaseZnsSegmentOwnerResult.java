package org.zstack.sdk.network.zns;

import org.zstack.sdk.network.zns.ZnsSegmentRefInventory;

public class ReleaseZnsSegmentOwnerResult {
    public ZnsSegmentRefInventory inventory;
    public void setInventory(ZnsSegmentRefInventory inventory) {
        this.inventory = inventory;
    }
    public ZnsSegmentRefInventory getInventory() {
        return this.inventory;
    }

}
