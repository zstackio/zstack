package org.zstack.sdk;

import org.zstack.sdk.PortMirrorInventory;

public class UpdatePortMirrorResult {
    public PortMirrorInventory inventory;
    public void setInventory(PortMirrorInventory inventory) {
        this.inventory = inventory;
    }
    public PortMirrorInventory getInventory() {
        return this.inventory;
    }

}
