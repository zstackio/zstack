package org.zstack.storage.primary.local;

import org.zstack.header.message.MessageReply;
import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by xing5 on 2016/7/21.
 */
public class MigrateVolumeOnLocalStorageReply extends MessageReply {
    private LocalStorageResourceRefInventory inventory;
    private ErrorCode networkError;

    public ErrorCode getNetworkError() {
        return networkError;
    }

    public void setNetworkError(ErrorCode networkError) {
        this.networkError = networkError;
    }

    public LocalStorageResourceRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(LocalStorageResourceRefInventory inventory) {
        this.inventory = inventory;
    }
}
