package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@RestResponse(allTo = "inventories")
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
        SchedulerInventory scheduler = new SchedulerInventory();
        scheduler.setUuid(uuid());
        scheduler.setSchedulerName("Test");
        scheduler.setSchedulerType("simple");
        scheduler.setRepeatCount(10);
        scheduler.setJobClassName("CreateVolumeSnapshotJob");
        scheduler.setState("Enabled");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setStartTime(new Timestamp(System.currentTimeMillis()));
        scheduler.setCreateDate(new Timestamp(System.currentTimeMillis()));
        scheduler.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setInventories(asList(scheduler));
        reply.setSuccess(true);
        return reply;
    }

}
