package org.zstack.header.core.external.plugin;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddFeaturePluginPackageEvent extends APIEvent {
    private FeaturePluginPackageInventory inventory;

    public APIAddFeaturePluginPackageEvent() { }

    public APIAddFeaturePluginPackageEvent(String apiId) {
        super(apiId);
    }

    public FeaturePluginPackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(FeaturePluginPackageInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddFeaturePluginPackageEvent __example__() {
        APIAddFeaturePluginPackageEvent evt = new APIAddFeaturePluginPackageEvent();
        FeaturePluginPackageInventory inv = FeaturePluginPackageInventory.__example__();
        inv.setPath("/usr/local/zstack/plugin-repo/flowMeter-5.5.48-1.zpkg");
        inv.setSource(FeaturePluginPackageInventory.SOURCE_UPLOAD);
        evt.setInventory(inv);
        return evt;
    }
}
