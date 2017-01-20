package org.zstack.header.vm;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by root on 8/16/16.
 */
@RestResponse(allTo = "inventory")
public class APICreateRebootVmInstanceSchedulerEvent extends APIEvent {
    private SchedulerInventory inventory;

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateRebootVmInstanceSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APICreateRebootVmInstanceSchedulerEvent() {
        super(null);
    }
 
    public static APICreateRebootVmInstanceSchedulerEvent __example__() {
        APICreateRebootVmInstanceSchedulerEvent event = new APICreateRebootVmInstanceSchedulerEvent();
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
        return event;
    }

}
