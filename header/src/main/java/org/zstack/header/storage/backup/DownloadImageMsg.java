package org.zstack.header.storage.backup;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;

@ApiTimeout(apiClasses = {APIAddImageMsg.class})
public class DownloadImageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private ImageInventory imageInventory;
    private String backupStorageUuid;
    private String format;
    private boolean inject;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public DownloadImageMsg(ImageInventory inventory, boolean inject) {
        super();
        this.imageInventory = inventory;
        this.inject = inject;
    }

    public DownloadImageMsg(ImageInventory inventory) {
        super();
        this.imageInventory = inventory;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public ImageInventory getImageInventory() {
        return imageInventory;
    }

    public void setImageInventory(ImageInventory imageInventory) {
        this.imageInventory = imageInventory;
    }

    public boolean isInject() {
        return inject;
    }

    public void setInject(boolean inject) {
        this.inject = inject;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }
}
