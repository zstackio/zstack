package org.zstack.sdk.disasterrecovery;

import org.zstack.sdk.disasterrecovery.DisasterRecoveryVmTemplateInventory;

public class UpdateDisasterRecoveryVmTemplateResult {
    public DisasterRecoveryVmTemplateInventory inventory;
    public void setInventory(DisasterRecoveryVmTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public DisasterRecoveryVmTemplateInventory getInventory() {
        return this.inventory;
    }

}
