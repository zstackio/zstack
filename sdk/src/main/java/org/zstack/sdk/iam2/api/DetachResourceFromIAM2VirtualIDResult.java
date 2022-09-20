package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2VirtualIDResourceRefInventory;

public class DetachResourceFromIAM2VirtualIDResult {
    public IAM2VirtualIDResourceRefInventory inventory;
    public void setInventory(IAM2VirtualIDResourceRefInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2VirtualIDResourceRefInventory getInventory() {
        return this.inventory;
    }

}
