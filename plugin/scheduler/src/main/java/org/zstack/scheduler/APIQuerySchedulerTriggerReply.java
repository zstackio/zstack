package org.zstack.scheduler;

import org.zstack.header.core.scheduler.SchedulerTriggerInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;


/**
 * Created by AlanJager on 2017/6/8.
 */
@RestResponse(allTo = "inventories")
public class APIQuerySchedulerTriggerReply  extends APIQueryReply {
    private List<SchedulerTriggerInventory> inventories;

    public List<SchedulerTriggerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerTriggerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySchedulerTriggerReply __example__() {
        APIQuerySchedulerTriggerReply reply = new APIQuerySchedulerTriggerReply();
        SchedulerTriggerInventory inv = new SchedulerTriggerInventory();
        inv.setUuid(uuid());
        inv.setName("test");
        inv.setDescription("a test trigger");
        inv.setStartTime(new Timestamp(System.currentTimeMillis()));
        inv.setStopTime(new Timestamp(System.currentTimeMillis()));
        inv.setCreateDate(new Timestamp(System.currentTimeMillis()));
        inv.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setInventories(asList(inv));
        return reply;
    }
}
