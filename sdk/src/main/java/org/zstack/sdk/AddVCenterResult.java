package org.zstack.sdk;

import org.zstack.sdk.VCenterInventory;
import org.zstack.sdk.SkippedResources;

public class AddVCenterResult {
    public VCenterInventory inventory;
    public void setInventory(VCenterInventory inventory) {
        this.inventory = inventory;
    }
    public VCenterInventory getInventory() {
        return this.inventory;
    }

    public SkippedResources skippedResources;
    public void setSkippedResources(SkippedResources skippedResources) {
        this.skippedResources = skippedResources;
    }
    public SkippedResources getSkippedResources() {
        return this.skippedResources;
    }

}
