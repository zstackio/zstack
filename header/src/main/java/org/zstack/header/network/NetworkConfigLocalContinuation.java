package org.zstack.header.network;

import org.zstack.header.core.Completion;

public interface NetworkConfigLocalContinuation {
    void run(NetworkConfigMutation mutation, Completion completion);
}
