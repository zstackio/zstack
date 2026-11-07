package org.zstack.sdk;

import org.zstack.sdk.FeaturePluginPackageInventory;

public class AddFeaturePluginPackageResult {
    public FeaturePluginPackageInventory inventory;
    public void setInventory(FeaturePluginPackageInventory inventory) {
        this.inventory = inventory;
    }
    public FeaturePluginPackageInventory getInventory() {
        return this.inventory;
    }

}
