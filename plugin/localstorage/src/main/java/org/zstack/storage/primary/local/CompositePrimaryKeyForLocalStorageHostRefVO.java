package org.zstack.storage.primary.local;

import java.io.Serializable;

/**
 * Created by frank on 6/30/2015.
 */
public class CompositePrimaryKeyForLocalStorageHostRefVO implements Serializable {
    private String hostUuid;
    private String primaryStorageUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
