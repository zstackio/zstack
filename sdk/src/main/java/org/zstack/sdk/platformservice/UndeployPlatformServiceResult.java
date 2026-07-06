package org.zstack.sdk.platformservice;

import org.zstack.sdk.platformservice.PlatformServiceInstanceInventory;

public class UndeployPlatformServiceResult {
    public PlatformServiceInstanceInventory inventory;
    public void setInventory(PlatformServiceInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public PlatformServiceInstanceInventory getInventory() {
        return this.inventory;
    }

}
