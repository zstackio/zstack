package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;

/**
 * Created by Mei Lei on 7/15/16.
 */
public class APIResumeSchedulerEvent extends APIEvent{
    private SchedulerInventory inventory;

    public APIResumeSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APIResumeSchedulerEvent() {
        super(null);
    }

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }
}
