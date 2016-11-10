package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmAccountPerference;

/**
 * Created by mingjian.deng on 16/10/18.
 */
public class ChangeVmPasswordMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String ip;
    private VmAccountPerference accountPerference;

    public VmAccountPerference getAccountPerference() { return accountPerference; }

    public void setAccountPerference(VmAccountPerference accountPerference) { this.accountPerference = accountPerference; }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
