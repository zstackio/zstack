package org.zstack.header.network;

import org.zstack.header.core.Completion;

public interface NetworkConfigChangeCoordinator {
    default String l2ApiConfigurationSyncSignature(String l2NetworkUuid) {
        return null;
    }

    boolean isApplicable(NetworkConfigChange change);

    void coordinate(NetworkConfigChange change,
                LocalNetworkConfigChange localChange,
                Completion completion);
}
