package org.zstack.header.network.l3;

public interface L3NetworkUpdateExtensionPoint {
    void updateL3NetworkMtu(L3NetworkInventory inventory);
}
