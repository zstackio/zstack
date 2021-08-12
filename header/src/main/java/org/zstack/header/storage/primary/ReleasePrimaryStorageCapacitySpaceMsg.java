package org.zstack.header.storage.primary;

public class ReleasePrimaryStorageCapacitySpaceMsg extends IncreasePrimaryStorageCapacityMsg{
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}