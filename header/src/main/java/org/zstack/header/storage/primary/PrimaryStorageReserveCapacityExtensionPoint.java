package org.zstack.header.storage.primary;

public interface PrimaryStorageReserveCapacityExtensionPoint {

    String getInstallPath(PrimaryStorageInventory psInv, AllocatePrimaryStorageMsg msg);

    void reserveCapacityHook(String installUrl, long size, String psUuid, boolean ignoreError);
}
