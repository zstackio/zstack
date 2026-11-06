package org.zstack.plugin.netprobe;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateNetProbeTaskEvent extends APIEvent {
    private NetProbeTaskInventory inventory;

    public APICreateNetProbeTaskEvent() {
    }

    public APICreateNetProbeTaskEvent(String apiId) {
        super(apiId);
    }

    public NetProbeTaskInventory getInventory() {
        return inventory;
    }

    public void setInventory(NetProbeTaskInventory inventory) {
        this.inventory = inventory;
    }
}
