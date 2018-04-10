package org.zstack.sdk;

import org.zstack.sdk.AliyunDiskInventory;

public class AttachAliyunDiskToEcsResult {
    public AliyunDiskInventory inventory;
    public void setInventory(AliyunDiskInventory inventory) {
        this.inventory = inventory;
    }
    public AliyunDiskInventory getInventory() {
        return this.inventory;
    }

}
