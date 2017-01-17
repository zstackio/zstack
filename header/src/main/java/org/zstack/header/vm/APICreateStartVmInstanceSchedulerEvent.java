package org.zstack.header.vm;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by root on 7/30/16.
 */

@RestResponse(allTo = "inventory")
public class APICreateStartVmInstanceSchedulerEvent extends APIEvent {
    private SchedulerInventory inventory;

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateStartVmInstanceSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APICreateStartVmInstanceSchedulerEvent() {
        super(null);
    }
 
    public static APICreateStartVmInstanceSchedulerEvent __example__() {
        APICreateStartVmInstanceSchedulerEvent event = new APICreateStartVmInstanceSchedulerEvent();


        return event;
    }

}
