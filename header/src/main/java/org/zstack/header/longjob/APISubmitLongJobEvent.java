package org.zstack.header.longjob;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 11/13/17.
 */
@RestResponse(allTo = "inventory")
public class APISubmitLongJobEvent extends APIEvent {
    private LongJobInventory inventory;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public APISubmitLongJobEvent() {
    }

    public APISubmitLongJobEvent(String apiId) {
        super(apiId);
    }

    public static APISubmitLongJobEvent __example__() {
        APISubmitLongJobEvent event = new APISubmitLongJobEvent();
        LongJobInventory inv = new LongJobInventory();
        inv.setUuid(uuid());
        event.setInventory(inv);
        return event;
    }
}
