package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.AllocatePrimaryStorageMsg;

/**
 * Created by frank on 7/2/2015.
 */
public class AllocateLocalStorageMsg extends AllocatePrimaryStorageMsg {
    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    @Override
    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
