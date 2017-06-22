package org.zstack.scheduler;

import org.zstack.header.core.scheduler.SchedulerJobInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@RestResponse(allTo = "inventories")
public class APIQuerySchedulerJobReply extends APIQueryReply {
    private List<SchedulerJobInventory> inventories;

    public List<SchedulerJobInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerJobInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySchedulerJobReply __example__() {
        APIQuerySchedulerJobReply reply = new APIQuerySchedulerJobReply();
        SchedulerJobInventory scheduler = new SchedulerJobInventory();
        scheduler.setUuid(uuid());
        scheduler.setName("test");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setCreateDate(new Timestamp(System.currentTimeMillis()));
        scheduler.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setInventories(asList(scheduler));
        reply.setSuccess(true);
        return reply;
    }

}
