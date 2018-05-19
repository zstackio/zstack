package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2VirtualIDInventory;

public class UpdateIAM2VirtualIDResult {
    public IAM2VirtualIDInventory inventory;
    public void setInventory(IAM2VirtualIDInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2VirtualIDInventory getInventory() {
        return this.inventory;
    }

}
