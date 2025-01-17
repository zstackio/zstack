package org.zstack.header.core.external.plugin;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.NeedReplyMessage;

public class DeletePluginDriversMsg extends NeedReplyMessage {
    private String uuid;
    private String deletionMode;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setDeletionMode(APIDeleteMessage.DeletionMode deletionMode) {
        this.deletionMode = deletionMode.toString();
    }

    public APIDeleteMessage.DeletionMode getDeletionMode() {
        return StringUtils.isEmpty(deletionMode) ? APIDeleteMessage.DeletionMode.Permissive : APIDeleteMessage.DeletionMode.valueOf(deletionMode);
    }

    public String getDeletionModeString() {
        return StringUtils.isEmpty(deletionMode) ? APIDeleteMessage.DeletionMode.Permissive.toString() : deletionMode;
    }

    public void setDeletionMode(String deletionMode) {
        this.deletionMode = deletionMode;
    }
}
