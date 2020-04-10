package org.zstack.header.network.l2;

public interface L2NetworkFactory {
    L2NetworkType getType();

    L2NetworkInventory createL2Network(L2NetworkVO vo, APICreateL2NetworkMsg msg);

    L2Network getL2Network(L2NetworkVO vo);
}
