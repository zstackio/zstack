package org.zstack.sdk;

import org.zstack.sdk.CdpPolicyInventory;

public class AttachCdpPolicyToVmInstanceResult {
    public CdpPolicyInventory inventory;
    public void setInventory(CdpPolicyInventory inventory) {
        this.inventory = inventory;
    }
    public CdpPolicyInventory getInventory() {
        return this.inventory;
    }

}
