package org.zstack.zcenter.accounts.api;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateSessionForZCenterAccountEvent extends APIEvent {
    private SessionInventory inventory;

    public APICreateSessionForZCenterAccountEvent() {
        super(null);
    }

    public APICreateSessionForZCenterAccountEvent(String apiId) {
        super(apiId);
    }

    public SessionInventory getInventory() {
        return inventory;
    }

    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateSessionForZCenterAccountEvent __example__() {
        APICreateSessionForZCenterAccountEvent event = new APICreateSessionForZCenterAccountEvent();
        event.setInventory(SessionInventory.__example__());
        return event;
    }
}
