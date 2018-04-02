package org.zstack.sdk.iam2.api;

import org.zstack.sdk.iam2.entity.IAM2VirtualIDGroupInventory;

public class ChangeIAM2VirtualIDGroupStateResult {
    public IAM2VirtualIDGroupInventory inventory;
    public void setInventory(IAM2VirtualIDGroupInventory inventory) {
        this.inventory = inventory;
    }
    public IAM2VirtualIDGroupInventory getInventory() {
        return this.inventory;
    }

}
