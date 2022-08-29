package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2ProjectInventory;

public class AttachIAM2ProjectToClusterResult {
    public IAM2ProjectInventory inventory;
    public void setInventory(IAM2ProjectInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2ProjectInventory getInventory() {
        return this.inventory;
    }

}
