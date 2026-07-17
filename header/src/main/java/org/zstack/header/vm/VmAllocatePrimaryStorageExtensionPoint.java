package org.zstack.header.vm;

import java.util.List;

public interface VmAllocatePrimaryStorageExtensionPoint {
    default void beforeAllocatePrimaryStorage(VmInstanceSpec spec) {
    }

    default void filterPrimaryStorageCandidates(VmInstanceSpec spec, List<String> rootPrimaryStorageUuids,
                                                boolean rootPrimaryStorageAutoAllocation,
                                                List<String> dataPrimaryStorageUuids,
                                                boolean dataPrimaryStorageAutoAllocation) {
    }
}
