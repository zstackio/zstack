package org.zstack.header.network;

import org.zstack.header.core.Completion;

public interface NetworkConfigMutationExtensionPoint {
    boolean supports(NetworkConfigMutation mutation);

    void mutate(NetworkConfigMutation mutation,
                NetworkConfigLocalContinuation continuation,
                Completion completion);
}
