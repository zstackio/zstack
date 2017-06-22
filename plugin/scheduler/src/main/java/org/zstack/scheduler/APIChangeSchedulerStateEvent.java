package org.zstack.scheduler;

import org.zstack.header.core.scheduler.SchedulerJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by Mei Lei on 8/31/16.
 */
@RestResponse(allTo = "inventory")
public class APIChangeSchedulerStateEvent extends APIEvent {
    private SchedulerJobInventory inventory;

    public APIChangeSchedulerStateEvent() {
        super(null);
    }

    public APIChangeSchedulerStateEvent(String apiId) {
        super(apiId);
    }

    public SchedulerJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeSchedulerStateEvent __example__() {
        APIChangeSchedulerStateEvent event = new APIChangeSchedulerStateEvent();
        SchedulerJobInventory scheduler = new SchedulerJobInventory();
        scheduler.setUuid(uuid());
        scheduler.setName("Test");
        scheduler.setDescription("Create volume snapshot scheduler job");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setCreateDate(new Timestamp(System.currentTimeMillis()));
        scheduler.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        event.setInventory(scheduler);
        event.setSuccess(true);
        return event;
    }

}
