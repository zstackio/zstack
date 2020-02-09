package org.zstack.header.vm;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUpdateVmOSEvent extends APIEvent {
    private LongJobInventory inventory;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateVmOSEvent() {}

    public APIUpdateVmOSEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateVmOSEvent __example__() {
        APIUpdateVmOSEvent event = new APIUpdateVmOSEvent();
        return event;
    }
}
