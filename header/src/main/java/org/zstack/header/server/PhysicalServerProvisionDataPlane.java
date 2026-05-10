package org.zstack.header.server;

import org.zstack.header.core.Completion;

public interface PhysicalServerProvisionDataPlane {
    ProvisionNetworkType getType();

    void provision(PhysicalServerProvisionTarget target, ProvisionPhase startPhase, Completion completion);
}
