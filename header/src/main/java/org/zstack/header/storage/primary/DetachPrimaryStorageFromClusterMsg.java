package org.zstack.header.storage.primary;

import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 */
public class DetachPrimaryStorageFromClusterMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String clusterUuid;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}
