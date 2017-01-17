package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateSystemTagEvent extends APIEvent {
    private SystemTagInventory inventory;

    public APICreateSystemTagEvent(String apiId) {
        super(apiId);
    }

    public APICreateSystemTagEvent() {
        super(null);
    }

    public SystemTagInventory getInventory() {
        return inventory;
    }

    public void setInventory(SystemTagInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateSystemTagEvent __example__() {
        APICreateSystemTagEvent event = new APICreateSystemTagEvent();


        return event;
    }

}
