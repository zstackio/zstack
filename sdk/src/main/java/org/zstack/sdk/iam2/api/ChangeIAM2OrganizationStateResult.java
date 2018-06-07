package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2OrganizationInventory;

public class ChangeIAM2OrganizationStateResult {
    public IAM2OrganizationInventory inventory;
    public void setInventory(IAM2OrganizationInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2OrganizationInventory getInventory() {
        return this.inventory;
    }

}
