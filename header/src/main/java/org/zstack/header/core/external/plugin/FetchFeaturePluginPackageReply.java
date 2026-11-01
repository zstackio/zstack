package org.zstack.header.core.external.plugin;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;

public class FetchFeaturePluginPackageReply extends MessageReply {
    private String packageName;
    @NoLogging
    private String content;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
