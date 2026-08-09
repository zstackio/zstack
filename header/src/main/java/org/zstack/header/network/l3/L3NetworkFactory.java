package org.zstack.header.network.l3;

import org.zstack.header.network.l2.NetworkCreateContext;

public interface L3NetworkFactory {
    L3NetworkType getType();

    L3NetworkInventory createL3Network(L3NetworkVO l3vo, APICreateL3NetworkMsg msg);

    default L3NetworkInventory createL3Network(L3NetworkVO l3vo, APICreateL3NetworkMsg msg, NetworkCreateContext context) {
        return createL3Network(l3vo, msg);
    }

    L3Network getL3Network(L3NetworkVO vo);

    boolean applyNetworkServiceWhenVmStateChange();
}
