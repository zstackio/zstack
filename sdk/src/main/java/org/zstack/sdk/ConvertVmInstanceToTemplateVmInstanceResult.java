package org.zstack.sdk;

import org.zstack.sdk.TemplateVmInstanceInventory;

public class ConvertVmInstanceToTemplateVmInstanceResult {
    public TemplateVmInstanceInventory inventory;
    public void setInventory(TemplateVmInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public TemplateVmInstanceInventory getInventory() {
        return this.inventory;
    }

}
