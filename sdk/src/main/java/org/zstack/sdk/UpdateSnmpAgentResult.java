package org.zstack.sdk;

import org.zstack.sdk.SnmpAgentInventory;

public class UpdateSnmpAgentResult {
    public SnmpAgentInventory inventory;
    public void setInventory(SnmpAgentInventory inventory) {
        this.inventory = inventory;
    }
    public SnmpAgentInventory getInventory() {
        return this.inventory;
    }

}
