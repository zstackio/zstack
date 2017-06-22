package org.zstack.scheduler;

import org.zstack.header.core.scheduler.SchedulerJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by AlanJager on 2017/6/10.
 */
@RestResponse(allTo = "inventory")
public class APICreateSchedulerJobEvent extends APIEvent {
    SchedulerJobInventory inventory;

    public APICreateSchedulerJobEvent() {
    }

    public APICreateSchedulerJobEvent(String apiId) {
        super(apiId);
    }

    public SchedulerJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobInventory inventory) {
        this.inventory = inventory;
    }
}
