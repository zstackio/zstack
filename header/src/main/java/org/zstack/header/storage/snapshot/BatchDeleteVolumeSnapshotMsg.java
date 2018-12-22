package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Create by weiwang at 2018-12-21
 */
public class BatchDeleteVolumeSnapshotMsg extends NeedReplyMessage {
    private List<String> uuids;

    private String volumeUuid;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
