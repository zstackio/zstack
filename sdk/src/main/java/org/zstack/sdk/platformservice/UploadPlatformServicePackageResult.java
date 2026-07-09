package org.zstack.sdk.platformservice;

import org.zstack.sdk.platformservice.PlatformServicePackageInventory;
import org.zstack.sdk.LongJobInventory;

public class UploadPlatformServicePackageResult {
    public PlatformServicePackageInventory inventory;
    public void setInventory(PlatformServicePackageInventory inventory) {
        this.inventory = inventory;
    }
    public PlatformServicePackageInventory getInventory() {
        return this.inventory;
    }

    public LongJobInventory longJobInventory;
    public void setLongJobInventory(LongJobInventory longJobInventory) {
        this.longJobInventory = longJobInventory;
    }
    public LongJobInventory getLongJobInventory() {
        return this.longJobInventory;
    }

}
