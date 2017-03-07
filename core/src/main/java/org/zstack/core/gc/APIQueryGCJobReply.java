package org.zstack.core.gc;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/3/5.
 */
@RestResponse(allTo = "inventories")
public class APIQueryGCJobReply extends APIQueryReply {
    private List<GarbageCollectorInventory> inventories;

    public List<GarbageCollectorInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GarbageCollectorInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryGCJobReply __example__() {
        APIQueryGCJobReply reply = new APIQueryGCJobReply();
        GarbageCollectorInventory gc = new GarbageCollectorInventory();
        gc.setName("TestGC");
        gc.setUuid(uuid());
        gc.setType("TimeBased");
        gc.setCreateDate(new Timestamp(System.currentTimeMillis()));
        gc.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setSuccess(true);
        reply.setInventories(asList(gc));
        return reply;
    }
}
