package org.zstack.header.vm;

import org.zstack.header.core.scheduler.SchedulerJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by root on 8/16/16.
 */
@RestResponse(allTo = "inventory")
public class APICreateRebootVmInstanceSchedulerEvent extends APIEvent {
    private SchedulerJobInventory inventory;

    public SchedulerJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobInventory inventory) {
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
