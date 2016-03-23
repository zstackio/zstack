package org.zstack.storage.primary.local;

import java.io.Serializable;

/**
 * Created by xing5 on 2016/3/23.
 */
public class GCDeleteBitsContext implements Serializable {
    private String primaryStorageUuid;
    private String hostUuid;
    private String installPath;

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

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
