package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
public class APIQuerySchedulerReply extends APIQueryReply {
    private List<SchedulerInventory> inventories;

    public List<SchedulerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySchedulerReply __example__() {
        APIQuerySchedulerReply reply = new APIQuerySchedulerReply();


        return reply;
    }

}
