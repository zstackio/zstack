package org.zstack.header.storage.primary;

public class IncreasePrimaryStorageCapacitySpaceMsg extends IncreasePrimaryStorageCapacityMsg{
    AllocatePrimaryStorageMsg allocatePrimaryStorageMsg;

    public AllocatePrimaryStorageMsg getAllocatePrimaryStorageMsg() {
        return allocatePrimaryStorageMsg;
    }

    public void setAllocatePrimaryStorageMsg(AllocatePrimaryStorageMsg allocatePrimaryStorageMsg) {
        this.allocatePrimaryStorageMsg = allocatePrimaryStorageMsg;
    }
}
