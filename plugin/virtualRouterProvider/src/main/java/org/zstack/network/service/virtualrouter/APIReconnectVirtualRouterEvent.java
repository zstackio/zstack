package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.message.APIEvent;

/**
 */
public class APIReconnectVirtualRouterEvent extends APIEvent {
    private ApplianceVmInventory inventory;

    public APIReconnectVirtualRouterEvent(String apiId) {
        super(apiId);
    }

    public APIReconnectVirtualRouterEvent() {
        super(null);
    }

    public ApplianceVmInventory getInventory() {
        return inventory;
    }

    public void setInventory(ApplianceVmInventory inventory) {
        this.inventory = inventory;
    }
}
