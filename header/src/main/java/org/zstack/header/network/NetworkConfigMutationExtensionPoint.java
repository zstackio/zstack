package org.zstack.header.network;

import org.zstack.header.core.Completion;

public interface NetworkConfigMutationExtensionPoint {
    void prepare(NetworkConfigMutation mutation, Completion completion);
}
