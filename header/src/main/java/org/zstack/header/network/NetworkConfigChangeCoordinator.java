package org.zstack.header.network;

import org.zstack.header.core.Completion;

public interface NetworkConfigChangeCoordinator {
    boolean isApplicable(NetworkConfigChange change);

    void coordinate(NetworkConfigChange change,
                LocalNetworkConfigChange localChange,
                Completion completion);
}
