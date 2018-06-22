package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Create by weiwang at 2018/6/12
 */
public class TakeVolumesSnapshotOnKvmReply extends MessageReply {
    private List<TakeSnapshotsOnKvmResultStruct> snapshotsResults;

    public List<TakeSnapshotsOnKvmResultStruct> getSnapshotsResults() {
        return snapshotsResults;
    }

    public void setSnapshotsResults(List<TakeSnapshotsOnKvmResultStruct> snapshotsResults) {
        this.snapshotsResults = snapshotsResults;
    }
}
