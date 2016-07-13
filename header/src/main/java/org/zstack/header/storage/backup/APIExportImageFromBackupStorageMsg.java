package org.zstack.header.storage.backup;

import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@Action(category = ImageConstant.ACTION_CATEGORY)
public class APIExportImageFromBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class, checkAccount = true, operationTarget = true)
    private String backupStorageUuid;

    @APIParam(nonempty = true, maxLength = 2048)
    private String installPath;

    @Override
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
}
