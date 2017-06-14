package org.zstack.header.volume;

import org.zstack.header.core.scheduler.SchedulerJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by root on 7/12/16.
 */
@RestResponse(allTo = "inventory")
public class APICreateVolumeSnapshotSchedulerEvent extends APIEvent {
    private SchedulerJobInventory inventory;

    public SchedulerJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobInventory inventory) {
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
        SchedulerJobInventory scheduler = new SchedulerJobInventory();
        scheduler.setUuid(uuid());
        scheduler.setName("Test");
        scheduler.setDescription("simple");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setCreateDate(new Timestamp(System.currentTimeMillis()));
        scheduler.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        event.setInventory(scheduler);
        return event;
    }

}
