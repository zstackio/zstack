package org.zstack.header.vm;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;

/**
 * Created by root on 7/30/16.
 */
public class APIStartVmInstanceSchedulerEvent extends APIEvent{
    private SchedulerInventory inventory;

    public SchedulerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerInventory inventory) {
        this.inventory = inventory;
    }

    public APIStartVmInstanceSchedulerEvent(String apiId) {
        super(apiId);
    }
    public APIStartVmInstanceSchedulerEvent() {
        super(null);
    }
}
