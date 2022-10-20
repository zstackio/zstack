package org.zstack.header.core;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class GetLocalTaskMsg extends NeedReplyMessage {
    private List<String> syncSignatures;

    public void setSyncSignatures(List<String> syncSignatures) {
        this.syncSignatures = syncSignatures;
    }

    public List<String> getSyncSignatures() {
        return syncSignatures;
    }
}