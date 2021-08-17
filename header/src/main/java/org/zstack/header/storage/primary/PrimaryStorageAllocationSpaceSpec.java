package org.zstack.header.storage.primary;

import java.util.List;

public class PrimaryStorageAllocationSpaceSpec extends PrimaryStorageAllocationSpec{
    private AllocatePrimaryStorageSpaceMsg allocationMessage;

    public AllocatePrimaryStorageSpaceMsg getAllocationMessage() {
        return allocationMessage;
    }

    public void setAllocationMessage(AllocatePrimaryStorageSpaceMsg allocationMessage) {
        this.allocationMessage = allocationMessage;
    }

}
