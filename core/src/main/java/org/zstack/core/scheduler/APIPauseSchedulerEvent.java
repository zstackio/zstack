package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;

/**
 * Created by Mei Lei on 7/15/16.
 */
public class APIPauseSchedulerEvent extends APIEvent{
    private SchedulerInventory inventory;

    public APIPauseSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APIPauseSchedulerEvent() {
        super(null);
    }

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }
}
