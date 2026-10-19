package org.zstack.header.network.l3;

import org.zstack.header.message.MessageReply;

public class SdnControllerEnableDHCPReply extends MessageReply {
    private SdnDhcpServiceSnapshot pendingDhcp;
    private boolean remoteWriteSucceeded;

    public SdnDhcpServiceSnapshot getPendingDhcp() {
        return pendingDhcp;
    }

    public void setPendingDhcp(SdnDhcpServiceSnapshot pendingDhcp) {
        this.pendingDhcp = pendingDhcp;
    }

    public boolean isRemoteWriteSucceeded() {
        return remoteWriteSucceeded;
    }

    public void setRemoteWriteSucceeded(boolean remoteWriteSucceeded) {
        this.remoteWriteSucceeded = remoteWriteSucceeded;
    }
}
