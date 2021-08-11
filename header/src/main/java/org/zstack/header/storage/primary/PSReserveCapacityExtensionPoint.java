package org.zstack.header.storage.primary;

import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

public interface PSReserveCapacityExtensionPoint {

    String getInstallUrl(PrimaryStorageInventory psInv, AllocatePrimaryStorageSpaceMsg msg);

    @Transactional(propagation = MANDATORY)
    void reserveCapacity(String installUrl, long size, String psUuid);

    void releaseCapacity(String installUrl, long size, String psUuid);
}
