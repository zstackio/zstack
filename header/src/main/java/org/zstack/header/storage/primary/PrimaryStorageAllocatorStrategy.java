package org.zstack.header.storage.primary;

import java.util.List;

public interface PrimaryStorageAllocatorStrategy {
    PrimaryStorageInventory allocate(PrimaryStorageAllocationSpec spec);

    List<PrimaryStorageInventory> allocateAllCandidates(PrimaryStorageAllocationSpec spec);
}
