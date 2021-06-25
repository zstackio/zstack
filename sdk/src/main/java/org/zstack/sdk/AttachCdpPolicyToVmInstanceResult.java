package org.zstack.sdk;

import org.zstack.sdk.CdpPolicyRefInventory;

public class AttachCdpPolicyToVmInstanceResult {
    public CdpPolicyRefInventory inventory;
    public void setInventory(CdpPolicyRefInventory inventory) {
        this.inventory = inventory;
    }
    public CdpPolicyRefInventory getInventory() {
        return this.inventory;
    }

}
