package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreatePolicyEvent extends APIEvent {
    private PolicyInventory inventory;

    public APICreatePolicyEvent(String apiId) {
        super(apiId);
    }

    public APICreatePolicyEvent() {
        super(null);
    }

    public PolicyInventory getInventory() {
        return inventory;
    }

    public void setInventory(PolicyInventory inventory) {
        this.inventory = inventory;
    }
}
