package org.zstack.image;

import java.io.Serializable;

/**
 * Created by david on 2/10/17.
 */
public class GCBitsDeletionOnBackupStorageContext implements Serializable {
    private String backupStorageUuid;
    private String installPath;
    private String imageUuid;

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
