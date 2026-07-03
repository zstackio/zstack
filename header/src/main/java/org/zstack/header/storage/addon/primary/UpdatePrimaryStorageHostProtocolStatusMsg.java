package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.storage.primary.UpdatePrimaryStorageHostStatusMsg;

import java.util.Map;

public class UpdatePrimaryStorageHostProtocolStatusMsg extends UpdatePrimaryStorageHostStatusMsg {
    private Map<String, PrimaryStorageHostStatus> protocolStatuses;

    public Map<String, PrimaryStorageHostStatus> getProtocolStatuses() {
        return protocolStatuses;
    }

    public void setProtocolStatuses(Map<String, PrimaryStorageHostStatus> protocolStatuses) {
        this.protocolStatuses = protocolStatuses;
    }
}
