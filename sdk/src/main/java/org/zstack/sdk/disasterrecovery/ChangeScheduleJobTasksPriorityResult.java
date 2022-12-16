package org.zstack.sdk.disasterrecovery;

import org.zstack.sdk.disasterrecovery.MirrorCdpTaskScheduleJobInventory;

public class ChangeScheduleJobTasksPriorityResult {
    public MirrorCdpTaskScheduleJobInventory inventory;
    public void setInventory(MirrorCdpTaskScheduleJobInventory inventory) {
        this.inventory = inventory;
    }
    public MirrorCdpTaskScheduleJobInventory getInventory() {
        return this.inventory;
    }

}
