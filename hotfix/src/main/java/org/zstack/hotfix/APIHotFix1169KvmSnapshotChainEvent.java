package org.zstack.hotfix;

import org.zstack.header.message.APIEvent;

import java.util.List;

/**
 * Created by xing5 on 2016/10/25.
 */
public class APIHotFix1169KvmSnapshotChainEvent extends APIEvent {
    private List<HotFix1169Result> results;

    public APIHotFix1169KvmSnapshotChainEvent() {
    }

    public APIHotFix1169KvmSnapshotChainEvent(String apiId) {
        super(apiId);
    }

    public List<HotFix1169Result> getResults() {
        return results;
    }

    public void setResults(List<HotFix1169Result> results) {
        this.results = results;
    }
}
