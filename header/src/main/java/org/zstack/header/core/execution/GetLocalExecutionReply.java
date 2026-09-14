package org.zstack.header.core.execution;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

public class GetLocalExecutionReply extends MessageReply {
    private List<ExecutionInventory> inventories = new ArrayList<>();

    public List<ExecutionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ExecutionInventory> inventories) {
        this.inventories = inventories;
    }
}
