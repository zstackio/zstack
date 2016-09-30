package org.zstack.header.core.workflow;

import java.util.Map;

public interface Flow {
    void run(FlowTrigger trigger, Map data);

    void rollback(FlowRollback trigger, Map data);
}
