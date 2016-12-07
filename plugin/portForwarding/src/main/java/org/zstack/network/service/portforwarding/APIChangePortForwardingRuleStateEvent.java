package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APIChangePortForwardingRuleStateEvent extends APIEvent {
    private PortForwardingRuleInventory inventory;

    public APIChangePortForwardingRuleStateEvent(String apiId) {
        super(apiId);
    }

    public APIChangePortForwardingRuleStateEvent() {
        super(null);
    }

    public PortForwardingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(PortForwardingRuleInventory inventory) {
        this.inventory = inventory;
    }
}
