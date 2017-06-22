package org.zstack.scheduler;

import org.zstack.header.core.scheduler.SchedulerJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateSchedulerJobEvent extends  APIEvent{
    private SchedulerJobInventory inventory;

    public APIUpdateSchedulerJobEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateSchedulerJobEvent() {
        super(null);
    }

    public SchedulerJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateSchedulerJobEvent __example__() {
        APIUpdateSchedulerJobEvent event = new APIUpdateSchedulerJobEvent();
        SchedulerJobInventory scheduler = new SchedulerJobInventory();
        scheduler.setUuid(uuid());
        scheduler.setName("Test");
        scheduler.setDescription("create volume snapshot job");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setCreateDate(new Timestamp(System.currentTimeMillis()));
        scheduler.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        event.setSuccess(true);
        event.setInventory(scheduler);
        return event;
    }

}
