package org.zstack.sdk;

import org.zstack.sdk.EcsSecurityGroupInventory;

public class CreateEcsSecurityGroupRemoteResult {
    public EcsSecurityGroupInventory inventory;
    public void setInventory(EcsSecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
    public EcsSecurityGroupInventory getInventory() {
        return this.inventory;
    }

}
