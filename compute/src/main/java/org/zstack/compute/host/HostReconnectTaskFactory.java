package org.zstack.compute.host;

import org.zstack.header.core.NoErrorCompletion;

public interface HostReconnectTaskFactory {
    HostReconnectTask createTask(String uuid, NoErrorCompletion completion);

    String getHypervisorType();
}
