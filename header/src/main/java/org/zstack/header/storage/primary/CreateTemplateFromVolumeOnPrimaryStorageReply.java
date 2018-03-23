package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class CreateTemplateFromVolumeOnPrimaryStorageReply extends MessageReply {
    private String templateBackupStorageInstallPath;
    private String format;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getTemplateBackupStorageInstallPath() {
        return templateBackupStorageInstallPath;
    }

    public void setTemplateBackupStorageInstallPath(String templateBackupStorageInstallPath) {
        this.templateBackupStorageInstallPath = templateBackupStorageInstallPath;
    }
}
