package org.zstack.header.network.l2;

/**
 * Created by boce.wang on 03/25/2024.
 */
public interface L2NetworkUpdateExtensionPoint {

    void beforeChangeL2NetworkVlanId(L2NetworkInventory l2Inv);

    void afterChangeL2NetworkVlanId(L2NetworkInventory l2Inv);
}
