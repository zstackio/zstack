package org.zstack.header.longjob;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APISuspendLongJobEvent extends APIEvent {
    private LongJobInventory inventory;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public APISuspendLongJobEvent() {
        super();
    }

    public APISuspendLongJobEvent(String apiId) {
        super(apiId);
    }

    public static APISuspendLongJobEvent __example__() {
        APISuspendLongJobEvent event = new APISuspendLongJobEvent();
        LongJobInventory inv = new LongJobInventory();
        inv.setUuid(uuid());
        inv.setState(LongJobState.Suspended);
        event.setInventory(inv);
        return event;
    }
}
