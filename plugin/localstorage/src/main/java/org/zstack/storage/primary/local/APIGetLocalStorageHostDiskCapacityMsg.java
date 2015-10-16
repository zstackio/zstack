package org.zstack.storage.primary.local;

import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by frank on 10/15/2015.
 */
public class APIGetLocalStorageHostDiskCapacityMsg extends APISyncCallMessage implements PrimaryStorageMessage {
    private String hostUuid;
    private String primaryStorageUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
