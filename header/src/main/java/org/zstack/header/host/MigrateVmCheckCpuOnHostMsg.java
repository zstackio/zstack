package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by LiangHanYu on 2021/7/29 15:27
 */
public class MigrateVmCheckCpuOnHostMsg extends NeedReplyMessage implements HostMessage{
    private String hostUuid;
    private String cpuXml;

    public String getCpuXml() {
        return cpuXml;
    }

    public void setCpuXml(String cpuXml) {
        this.cpuXml = cpuXml;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
