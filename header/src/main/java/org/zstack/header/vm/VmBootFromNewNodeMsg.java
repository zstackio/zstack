package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmBootFromNewNodeMsg extends NeedReplyMessage implements HostMessage {
    private String vmUuid;
    private String platformId;
    private String prvSocId;
    private String uuid;

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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }
}
