package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

public class GetVmVirtualizerVersionReply extends MessageReply {
    private String virtualizer;
    private String version;

    public String getVirtualizer() {
        return virtualizer;
    }

    public void setVirtualizer(String virtualizer) {
        this.virtualizer = virtualizer;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
