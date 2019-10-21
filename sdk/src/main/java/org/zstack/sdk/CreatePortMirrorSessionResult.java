package org.zstack.sdk;

import org.zstack.sdk.PortMirrorSessionInventory;

public class CreatePortMirrorSessionResult {
    public PortMirrorSessionInventory inventory;
    public void setInventory(PortMirrorSessionInventory inventory) {
        this.inventory = inventory;
    }
    public PortMirrorSessionInventory getInventory() {
        return this.inventory;
    }

}
