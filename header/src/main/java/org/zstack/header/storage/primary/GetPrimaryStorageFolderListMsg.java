package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2018/1/26.
 */
public class GetPrimaryStorageFolderListMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String path;
    private String hostUuid;
    private String primaryStorageUuid;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
