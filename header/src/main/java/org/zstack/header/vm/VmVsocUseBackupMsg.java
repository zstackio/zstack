package org.zstack.header.vm;

import org.zstack.header.host.Host;
import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmVsocUseBackupMsg extends NeedReplyMessage implements HostMessage {
    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    private String hostUuid;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    private String platformId;

    public String getBackupUuid() {
        return backupUuid;
    }

    public void setBackupUuid(String backupUuid) {
        this.backupUuid = backupUuid;
    }

    private String backupUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    private String vmUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
