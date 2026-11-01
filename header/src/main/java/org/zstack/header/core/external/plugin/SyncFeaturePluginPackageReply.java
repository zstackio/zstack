package org.zstack.header.core.external.plugin;

import org.zstack.header.message.MessageReply;

public class SyncFeaturePluginPackageReply extends MessageReply {
    private boolean needRestart;

    public boolean isNeedRestart() {
        return needRestart;
    }

    public void setNeedRestart(boolean needRestart) {
        this.needRestart = needRestart;
    }
}
