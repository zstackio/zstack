package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2ProjectTemplateInventory;

public class CreateIAM2ProjectTemplateResult {
    public IAM2ProjectTemplateInventory inventory;
    public void setInventory(IAM2ProjectTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2ProjectTemplateInventory getInventory() {
        return this.inventory;
    }

}
