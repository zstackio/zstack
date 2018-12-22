package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Create by weiwang at 2018-12-21
 */
public class BatchDeleteVolumeSnapshotReply extends MessageReply {
    private List<BatchDeleteVolumeSnapshotStruct> results;

    public List<BatchDeleteVolumeSnapshotStruct> getResults() {
        return results;
    }

    public void setResults(List<BatchDeleteVolumeSnapshotStruct> results) {
        this.results = results;
    }
}
