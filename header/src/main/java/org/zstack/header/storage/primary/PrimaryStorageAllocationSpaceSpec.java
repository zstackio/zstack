package org.zstack.header.storage.primary;

public class PrimaryStorageAllocationSpaceSpec extends PrimaryStorageAllocationSpec{
    private AllocatePrimaryStorageSpaceMsg allocationSpaceMessage;

    public AllocatePrimaryStorageSpaceMsg getAllocationSpaceMessage() {
        return allocationSpaceMessage;
    }

    public void setAllocationSpaceMessage(AllocatePrimaryStorageSpaceMsg allocationSpaceMessage) {
        this.allocationSpaceMessage = allocationSpaceMessage;
    }
}
