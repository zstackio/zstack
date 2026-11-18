package org.zstack.portal.managementnode;

import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;

public class CollectManagementNodeCpuTopologyReply extends MessageReply {
    private PhysicalServerCpuTopology topology;

    public PhysicalServerCpuTopology getTopology() {
        return topology;
    }

    public void setTopology(PhysicalServerCpuTopology topology) {
        this.topology = topology;
    }
}
