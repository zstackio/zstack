package org.zstack.scheduler;

import org.zstack.header.core.scheduler.SchedulerTriggerInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/8.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateSchedulerTriggerEvent extends APIEvent {
    private SchedulerTriggerInventory inventory;

    public APIUpdateSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateSchedulerTriggerEvent() {
        super(null);
    }

    public SchedulerTriggerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerTriggerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateSchedulerTriggerEvent __example__() {
        APIUpdateSchedulerTriggerEvent evt = new APIUpdateSchedulerTriggerEvent();
        SchedulerTriggerInventory inv = new SchedulerTriggerInventory();
        inv.setName("trigger");
        inv.setDescription("this is a scheduler trigger");
        inv.setUuid(uuid());
        inv.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        inv.setCreateDate(new Timestamp(System.currentTimeMillis()));

        return evt;
    }
}
