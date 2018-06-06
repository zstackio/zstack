package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2VirtualIDAttributeInventory;

public class UpdateIAM2VirtualIDAttributeResult {
    public IAM2VirtualIDAttributeInventory inventory;
    public void setInventory(IAM2VirtualIDAttributeInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2VirtualIDAttributeInventory getInventory() {
        return this.inventory;
    }

}
