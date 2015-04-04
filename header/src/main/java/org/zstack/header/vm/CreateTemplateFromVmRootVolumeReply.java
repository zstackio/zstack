package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

public class CreateTemplateFromVmRootVolumeReply extends MessageReply {
    private String installPath;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
