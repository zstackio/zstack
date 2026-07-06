package org.zstack.sdk.platformservice;

import org.zstack.sdk.platformservice.PlatformServicePackageInventory;

public class UploadPlatformServicePackageResult {
    public PlatformServicePackageInventory inventory;
    public void setInventory(PlatformServicePackageInventory inventory) {
        this.inventory = inventory;
    }
    public PlatformServicePackageInventory getInventory() {
        return this.inventory;
    }

}
