package org.zstack.header.server;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;

public interface ProvisionProvider {
    ProvisionNetworkType getType();

    void prepareNetwork(PhysicalServerProvisionNetworkInventory network, String poolUuid, Completion completion);

    void destroyNetwork(PhysicalServerProvisionNetworkInventory network, String poolUuid, Completion completion);

    void startProvisioning(ProvisionRequest request, ReturnValueCompletion<ProvisionResult> completion);
}
