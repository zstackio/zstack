package org.zstack.sdk.sns.platform.plugin;

import org.zstack.sdk.sns.platform.plugin.SNSPluginPlatformInventory;

public class UpdateSNSPluginPlatformResult {
    public SNSPluginPlatformInventory inventory;
    public void setInventory(SNSPluginPlatformInventory inventory) {
        this.inventory = inventory;
    }
    public SNSPluginPlatformInventory getInventory() {
        return this.inventory;
    }

}
