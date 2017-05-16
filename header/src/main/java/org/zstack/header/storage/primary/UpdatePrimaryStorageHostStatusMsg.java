package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by Administrator on 2017-05-12.
 */
public class UpdatePrimaryStorageHostStatusMsg extends NeedReplyMessage implements PrimaryStorageMessage{
    private List<String> primaryStorageUuids;
    private String hostUuid;
    private PrimaryStorageHostStatus status;

    public void setStatus(PrimaryStorageHostStatus status) {
        this.status = status;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public void setPrimaryStorageUuids(List<String> primaryStorageUuids) {
        this.primaryStorageUuids = primaryStorageUuids;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public List<String> getPrimaryStorageUuids() {
        return primaryStorageUuids;
    }

    public PrimaryStorageHostStatus getStatus() {
        return status;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuids.get(0);
    }
}
