package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventories", "total", "nextPage", "snapshotId"})
public class APIQueryMemoryStateReply extends APIReply {
    private String snapshotId;
    public String getSnapshotId() { return snapshotId; }
    public void setSnapshotId(String value) { snapshotId = value; }
    private Integer nextPage;
    public Integer getNextPage() { return nextPage; }
    public void setNextPage(Integer value) { nextPage = value; }
    private java.util.List<MemoryStateInventory> inventories;
    public java.util.List<MemoryStateInventory> getInventories() { return inventories; }
    public void setInventories(java.util.List<MemoryStateInventory> value) { inventories = value; }

    private long total;
    public long getTotal() { return total; }
    public void setTotal(long value) { total = value; }

    public static APIQueryMemoryStateReply __example__() {
        APIQueryMemoryStateReply reply = new APIQueryMemoryStateReply();
        reply.setSnapshotId("snapshot-example-1");
        reply.setNextPage(null);
        reply.setInventories(java.util.Collections.singletonList(MemoryStateInventory.__example__()));
        reply.setTotal(1);
        return reply;
    }
}
