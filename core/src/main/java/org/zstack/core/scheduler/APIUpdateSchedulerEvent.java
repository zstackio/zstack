package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateSchedulerEvent extends  APIEvent{
    private SchedulerInventory inventory;

    public APIUpdateSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateSchedulerEvent() {
        super(null);
    }

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateSchedulerEvent __example__() {
        APIUpdateSchedulerEvent event = new APIUpdateSchedulerEvent();


        return event;
    }

}
