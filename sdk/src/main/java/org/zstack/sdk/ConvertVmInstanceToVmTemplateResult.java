package org.zstack.sdk;

import org.zstack.sdk.VmTemplateInventory;

public class ConvertVmInstanceToVmTemplateResult {
    public VmTemplateInventory inventory;
    public void setInventory(VmTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public VmTemplateInventory getInventory() {
        return this.inventory;
    }

}
