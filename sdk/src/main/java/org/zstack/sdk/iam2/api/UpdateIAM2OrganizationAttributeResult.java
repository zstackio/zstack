package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2OrganizationAttributeInventory;

public class UpdateIAM2OrganizationAttributeResult {
    public IAM2OrganizationAttributeInventory inventory;
    public void setInventory(IAM2OrganizationAttributeInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2OrganizationAttributeInventory getInventory() {
        return this.inventory;
    }

}
