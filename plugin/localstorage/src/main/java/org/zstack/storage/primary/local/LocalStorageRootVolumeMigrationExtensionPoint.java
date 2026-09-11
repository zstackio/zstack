package org.zstack.storage.primary.local;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmInstanceInventory;

public interface LocalStorageRootVolumeMigrationExtensionPoint {
    default void preMigrateRootVolume(VmInstanceInventory vm, String sourceHostUuid,
                                      String targetHostUuid, Completion completion) {
        completion.success();
    }

    default void finalizeMigrateRootVolume(VmInstanceInventory vm, String sourceHostUuid,
                                           String targetHostUuid, Completion completion) {
        completion.success();
    }
}
