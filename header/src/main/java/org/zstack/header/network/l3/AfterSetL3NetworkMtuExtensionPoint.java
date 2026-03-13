package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;

public interface AfterSetL3NetworkMtuExtensionPoint {
    void afterSetL3NetworkMtu(L3NetworkInventory l3, int mtu, Completion completion);
}
