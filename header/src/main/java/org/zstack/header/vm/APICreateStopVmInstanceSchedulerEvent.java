package org.zstack.header.vm;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by root on 7/30/16.
 */
@RestResponse(allTo = "inventory")
public class APICreateStopVmInstanceSchedulerEvent extends APIEvent {
    private SchedulerInventory inventory;

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateStopVmInstanceSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APICreateStopVmInstanceSchedulerEvent() {
        super(null);
    }
 
    public static APICreateStopVmInstanceSchedulerEvent __example__() {
        APICreateStopVmInstanceSchedulerEvent event = new APICreateStopVmInstanceSchedulerEvent();
        SchedulerInventory scheduler = new SchedulerInventory();
        scheduler.setUuid(uuid());
        scheduler.setSchedulerName("Test");
        scheduler.setSchedulerType("simple");
        scheduler.setRepeatCount(10);
        scheduler.setJobClassName("StopVmInstanceJob");
        scheduler.setState("Enabled");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setStartTime(new Timestamp(System.currentTimeMillis()));
        scheduler.setCreateDate(new Timestamp(System.currentTimeMillis()));
        scheduler.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        event.setInventory(scheduler);

        return event;
    }

}
