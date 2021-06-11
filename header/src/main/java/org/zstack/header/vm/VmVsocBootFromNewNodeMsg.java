package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmVsocBootFromNewNodeMsg extends NeedReplyMessage implements HostMessage {
    private String vmUuid;
    private String platformId;
    private String prvSocId;
    private String hostUuid;

    public String getPrvSocId() {
        return prvSocId;
    }

    public void setPrvSocId(String prvSocId) {
        this.prvSocId = prvSocId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }


    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
