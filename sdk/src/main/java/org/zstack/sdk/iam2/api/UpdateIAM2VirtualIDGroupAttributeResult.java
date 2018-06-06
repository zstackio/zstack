package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2VirtualIDGroupAttributeInventory;

public class UpdateIAM2VirtualIDGroupAttributeResult {
    public IAM2VirtualIDGroupAttributeInventory inventory;
    public void setInventory(IAM2VirtualIDGroupAttributeInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2VirtualIDGroupAttributeInventory getInventory() {
        return this.inventory;
    }

}
