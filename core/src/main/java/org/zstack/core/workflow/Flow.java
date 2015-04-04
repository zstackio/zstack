package org.zstack.core.workflow;

import java.util.Map;

public interface Flow {
    void run(FlowTrigger trigger, Map data);

    void rollback(FlowTrigger trigger, Map data);
}
