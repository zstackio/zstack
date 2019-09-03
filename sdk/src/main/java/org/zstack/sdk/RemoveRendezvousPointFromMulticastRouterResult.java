package org.zstack.sdk;

import org.zstack.sdk.MulticastRouterInventory;

public class RemoveRendezvousPointFromMulticastRouterResult {
    public MulticastRouterInventory inventory;
    public void setInventory(MulticastRouterInventory inventory) {
        this.inventory = inventory;
    }
    public MulticastRouterInventory getInventory() {
        return this.inventory;
    }

}
