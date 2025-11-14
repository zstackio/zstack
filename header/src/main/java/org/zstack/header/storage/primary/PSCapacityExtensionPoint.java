package org.zstack.header.storage.primary;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface PSCapacityExtensionPoint {
    /**
     * Allocate space dry run, return the installUrl that can be used to allocate space
     * note: this method will not actually allocate space, just check whether the space can be allocated,
     * and it has no thread safety guarantee, so it is possible that the space can be allocated in dry run,
     * but fail in actual allocation.
     * @param msg
     * @param psInv
     * @return installUrl that can be used to allocate space
     */

    String allocateSpaceDryRun(AllocatePrimaryStorageSpaceMsg msg, PrimaryStorageInventory psInv);

    /**
     * Reserve capacity after space allocation.
     * throw exception if failed
     * @param msg
     * @param allocatedInstallUrl
     * @param size
     * @param psUuid
     * @return the actually reserved size
     */
    @Transactional(propagation = Propagation.MANDATORY)
    long reserveCapacity(AllocatePrimaryStorageSpaceMsg msg, String allocatedInstallUrl, long size, String psUuid);

    @Transactional(propagation = Propagation.MANDATORY)
    void releaseCapacity(String allocatedInstallUrl, long size, String psUuid);

    PrimaryStorageType getPrimaryStorageType();
}
