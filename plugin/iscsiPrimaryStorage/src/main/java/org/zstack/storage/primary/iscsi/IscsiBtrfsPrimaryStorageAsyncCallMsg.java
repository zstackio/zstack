package org.zstack.storage.primary.iscsi;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by frank on 6/10/2015.
 */
public class IscsiBtrfsPrimaryStorageAsyncCallMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String command;
    private String path;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(Object command) {
        this.command = JSONObjectUtil.toJsonString(command);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
