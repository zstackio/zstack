package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotReply.CreateTemplateFromVolumeSnapshotResult;

import java.util.List;

/**
 */
public class CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply extends MessageReply {
    private List<CreateTemplateFromVolumeSnapshotResult> results;

    public List<CreateTemplateFromVolumeSnapshotResult> getResults() {
        return results;
    }

    public void setResults(List<CreateTemplateFromVolumeSnapshotResult> results) {
        this.results = results;
    }

    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
