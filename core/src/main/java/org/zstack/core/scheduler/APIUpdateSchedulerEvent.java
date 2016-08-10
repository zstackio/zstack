package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;
/**
 * Created by root on 7/18/16.
 */
public class APIUpdateSchedulerEvent extends  APIEvent{
    private SchedulerInventory inventory;

    public APIUpdateSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateSchedulerEvent() {
        super(null);
    }

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }
}
