package org.zstack.header.storage.primary;

public interface PrimaryStorageReserveCapacityExtensionPoint {

    String getInstallUrl(PrimaryStorageInventory psInv, AllocatePrimaryStorageMsg msg);

    void reserveCapacityHook(String installUrl, long size, String psUuid, boolean ignoreError);
}
