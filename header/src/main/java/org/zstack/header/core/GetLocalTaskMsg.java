package org.zstack.header.core;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class GetLocalTaskMsg extends NeedReplyMessage {
    private List<String> syncSignatures;
    private boolean onlyRunningTask;

    public boolean isOnlyRunningTask() {
        return onlyRunningTask;
    }

    public void setOnlyRunningTask(boolean onlyRunningTask) {
        this.onlyRunningTask = onlyRunningTask;
    }

    public void setSyncSignatures(List<String> syncSignatures) {
        this.syncSignatures = syncSignatures;
    }

    public List<String> getSyncSignatures() {
        return syncSignatures;
    }
}