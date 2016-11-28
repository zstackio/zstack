package org.zstack.header.network.l3;

public interface L3NetworkFactory {
    L3NetworkType getType();

    L3NetworkInventory createL3Network(L3NetworkVO l3vo, APICreateL3NetworkMsg msg);

    L3Network getL3Network(L3NetworkVO vo);
}
