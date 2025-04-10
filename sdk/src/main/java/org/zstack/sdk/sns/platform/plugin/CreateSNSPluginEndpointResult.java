package org.zstack.sdk.sns.platform.plugin;

import org.zstack.sdk.sns.platform.plugin.SNSPluginEndpointInventory;

public class CreateSNSPluginEndpointResult {
    public SNSPluginEndpointInventory inventory;
    public void setInventory(SNSPluginEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSPluginEndpointInventory getInventory() {
        return this.inventory;
    }

}
