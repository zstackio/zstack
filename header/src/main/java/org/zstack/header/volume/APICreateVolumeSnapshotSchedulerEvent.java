package org.zstack.header.volume;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

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
}
