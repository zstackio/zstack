package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.volume.VolumeInventory;

public class CreateTemplateFromVolumeOnPrimaryStorageReply extends APIReply {
    private String templateBackupStorageInstallPath;

    public String getTemplateBackupStorageInstallPath() {
        return templateBackupStorageInstallPath;
    }

    public void setTemplateBackupStorageInstallPath(String templateBackupStorageInstallPath) {
        this.templateBackupStorageInstallPath = templateBackupStorageInstallPath;
    }
}
