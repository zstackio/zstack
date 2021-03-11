package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmVsocDeleteBackupMsg extends NeedReplyMessage implements HostMessage {
    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    private String vmUuid;

    public String getBackUuid() {
        return backUuid;
    }

    public void setBackUuid(String backUuid) {
        this.backUuid = backUuid;
    }

    private String backUuid;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    private String platformId;

    public void setHotUuid(String hotUuid) {
        this.hotUuid = hotUuid;
    }

    private String hotUuid;

    @Override
    public String getHostUuid() {
        return hotUuid;
    }
}

