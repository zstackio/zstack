package org.zstack.header.vm;

import java.util.List;

public interface VmAllocatePrimaryStorageExtensionPoint {
    default void filterPrimaryStorageCandidates(VmInstanceSpec spec, List<String> rootPrimaryStorageUuids,
                                                boolean rootPrimaryStorageAutoAllocation) {
    }
}
