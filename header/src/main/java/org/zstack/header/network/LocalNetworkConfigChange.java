package org.zstack.header.network;

import org.zstack.header.core.Completion;

public interface LocalNetworkConfigChange {
    void apply(Completion completion);
}
