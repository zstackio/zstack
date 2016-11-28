package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;

public interface L2NetworkRealizationExtensionPoint {
    void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion);

    void check(L2NetworkInventory l2Network, String hostUuid, Completion completion);

    L2NetworkType getSupportedL2NetworkType();

    HypervisorType getSupportedHypervisorType();
}
