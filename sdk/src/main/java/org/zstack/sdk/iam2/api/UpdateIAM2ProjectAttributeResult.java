package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2ProjectAttributeInventory;

public class UpdateIAM2ProjectAttributeResult {
    public IAM2ProjectAttributeInventory inventory;
    public void setInventory(IAM2ProjectAttributeInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2ProjectAttributeInventory getInventory() {
        return this.inventory;
    }

}
