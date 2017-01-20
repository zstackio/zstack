package org.zstack.header.volume;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by root on 7/12/16.
 */
@RestResponse(allTo = "inventory")
public class APICreateVolumeSnapshotSchedulerEvent extends APIEvent {
    private SchedulerInventory inventory;

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateVolumeSnapshotSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APICreateVolumeSnapshotSchedulerEvent() {
        super(null);
    }
 
    public static APICreateVolumeSnapshotSchedulerEvent __example__() {
        APICreateVolumeSnapshotSchedulerEvent event = new APICreateVolumeSnapshotSchedulerEvent();
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
