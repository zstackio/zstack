package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by Mei Lei on 8/31/16.
 */
@RestResponse(allTo = "inventory")
public class APIChangeSchedulerStateEvent extends APIEvent {
    private SchedulerInventory inventory;

    public APIChangeSchedulerStateEvent() {
        super(null);
    }

    public APIChangeSchedulerStateEvent(String apiId) {
        super(apiId);
    }

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeSchedulerStateEvent __example__() {
        APIChangeSchedulerStateEvent event = new APIChangeSchedulerStateEvent();
        SchedulerInventory scheduler = new SchedulerInventory();
        scheduler.setUuid(uuid());
        scheduler.setSchedulerName("Test");
        scheduler.setSchedulerType("simple");
        scheduler.setRepeatCount(10);
        scheduler.setJobClassName("CreateVolumeSnapshotJob");
        scheduler.setState("Enabled");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setStartTime(new Timestamp(System.currentTimeMillis()));
        scheduler.setCreateDate(new Timestamp(System.currentTimeMillis()));
        scheduler.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        event.setInventory(scheduler);
        event.setSuccess(true);
        return event;
    }

}
