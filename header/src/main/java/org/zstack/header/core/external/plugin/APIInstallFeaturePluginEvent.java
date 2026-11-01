package org.zstack.header.core.external.plugin;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIInstallFeaturePluginEvent extends APIEvent {
    private FeaturePluginPackageInventory inventory;

    public APIInstallFeaturePluginEvent() { }

    public APIInstallFeaturePluginEvent(String apiId) {
        super(apiId);
    }

    public FeaturePluginPackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(FeaturePluginPackageInventory inventory) {
        this.inventory = inventory;
    }

    public static APIInstallFeaturePluginEvent __example__() {
        APIInstallFeaturePluginEvent evt = new APIInstallFeaturePluginEvent();
        FeaturePluginPackageInventory inv = FeaturePluginPackageInventory.__example__();
        inv.setInstalled(true);
        evt.setInventory(inv);
        return evt;
    }
}
