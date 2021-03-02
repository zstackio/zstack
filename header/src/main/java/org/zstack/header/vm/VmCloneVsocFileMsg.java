package org.zstack.header.vm;

import org.springframework.core.SpringVersion;
import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmCloneVsocFileMsg extends NeedReplyMessage implements HostMessage {
    private String srcVmUuid;
    private String destVmUuid;
    private String destSocId;
    private int type;
    private int resource;
    private String hostUuid;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    private String platformId;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getDestSocId() {
        return destSocId;
    }

    public void setDestSocId(String destSocId) {
        this.destSocId = destSocId;
    }

    public int getResource() {
        return resource;
    }

    public void setResource(int resource) {
        this.resource = resource;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDestVmUuid() {
        return destVmUuid;
    }

    public void setDestVmUuid(String destVmUuid) {
        this.destVmUuid = destVmUuid;
    }

    public String getSrcVmUuid() {
        return srcVmUuid;
    }

    public void setSrcVmUuid(String srcVmUuid) {
        this.srcVmUuid = srcVmUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
