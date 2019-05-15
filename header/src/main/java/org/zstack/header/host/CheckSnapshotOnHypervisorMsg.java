package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2019/4/12.
 */
public class CheckSnapshotOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String volumeInstallPath;
    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
    }
}
