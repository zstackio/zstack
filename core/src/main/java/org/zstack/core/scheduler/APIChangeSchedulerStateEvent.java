package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;

/**
 * Created by Mei Lei on 8/31/16.
 */
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


        return event;
    }

}
