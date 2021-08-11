package org.zstack.header.storage.primary;

public class ReleasePrimaryStorageCapacitySpaceMsg extends IncreasePrimaryStorageCapacityMsg{
    AllocatePrimaryStorageSpaceMsg allocatePrimaryStorageSpaceMsg;

    public AllocatePrimaryStorageSpaceMsg getAllocatePrimaryStorageSpaceMsg() {
        return allocatePrimaryStorageSpaceMsg;
    }

    public void setAllocatePrimaryStorageSpaceMsg(AllocatePrimaryStorageSpaceMsg allocatePrimaryStorageSpaceMsg) {
        this.allocatePrimaryStorageSpaceMsg = allocatePrimaryStorageSpaceMsg;
    }
}