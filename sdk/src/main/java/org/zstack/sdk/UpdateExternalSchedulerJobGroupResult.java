package org.zstack.sdk;

import org.zstack.sdk.SchedulerJobGroupInventory;

public class UpdateExternalSchedulerJobGroupResult {
    public SchedulerJobGroupInventory inventory;
    public void setInventory(SchedulerJobGroupInventory inventory) {
        this.inventory = inventory;
    }
    public SchedulerJobGroupInventory getInventory() {
        return this.inventory;
    }

}
