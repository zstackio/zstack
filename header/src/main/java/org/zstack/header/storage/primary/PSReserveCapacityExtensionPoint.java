package org.zstack.header.storage.primary;

import org.springframework.transaction.annotation.Propagation;

import javax.transaction.Transactional;

public interface PSReserveCapacityExtensionPoint {
    String getInstallUrl(PrimaryStorageInventory psInv, AllocatePrimaryStorageSpaceMsg msg);

    @Transactional(propagation = Propagation.MANDATORY)
    void reserveCapacity(String installUrl, long size, String psUuid);

    void releaseCapacity(String installUrl, long size, String psUuid);
}
