package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmVsocCreateFromSnapshotMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmUuid;
    private String platformId;
    private String srcVmUuid;
    private String snapshotUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }


    public String getSrcVmUuid() {
        return srcVmUuid;
    }

    public void setSrcVmUuid(String srcVmUuid) {
        this.srcVmUuid = srcVmUuid;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
    
    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
