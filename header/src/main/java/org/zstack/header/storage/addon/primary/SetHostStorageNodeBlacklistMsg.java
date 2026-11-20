package org.zstack.header.storage.addon.primary;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class SetHostStorageNodeBlacklistMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String primaryStorageUuid;
    private boolean blacklist;

    @Override
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

    public boolean isBlacklist() {
        return blacklist;
    }

    public void setBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
    }
}
