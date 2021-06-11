package org.zstack.header.vm;

import org.zstack.header.host.Host;
import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmVsocCreateVmFromBackupMsg extends NeedReplyMessage implements HostMessage {
    public String getSrcVmUuid() {
        return srcVmUuid;
    }

    public void setSrcVmUuid(String srcVmUuid) {
        this.srcVmUuid = srcVmUuid;
    }

    private String srcVmUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    private String vmUuid;

    public String getBackupUuid() {
        return backupUuid;
    }

    public void setBackupUuid(String backupUuid) {
        this.backupUuid = backupUuid;
    }

    private String backupUuid;

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

    private String hostUuid;
    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
