package org.zstack.header.core.execution;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"inventories", "total", "nextCursor"})
public class APIQueryExecutionReply extends APIQueryReply {
    private List<ExecutionInventory> inventories = new ArrayList<>();
    private String nextCursor;

    public List<ExecutionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ExecutionInventory> inventories) {
        this.inventories = inventories;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public static APIQueryExecutionReply __example__() {
        APIQueryExecutionReply reply = new APIQueryExecutionReply();
        reply.setInventories(new ArrayList<ExecutionInventory>());
        reply.setTotal(0);
        return reply;
    }
}
