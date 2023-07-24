package org.zstack.sdk;

import org.zstack.sdk.SnmpAgentConfigInventory;

public class UpdateSnmpAgentResult {
    public SnmpAgentConfigInventory inventory;
    public void setInventory(SnmpAgentConfigInventory inventory) {
        this.inventory = inventory;
    }
    public SnmpAgentConfigInventory getInventory() {
        return this.inventory;
    }

}
