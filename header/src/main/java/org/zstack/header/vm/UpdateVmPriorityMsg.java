package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 20:13 2019/9/18
 */
public class UpdateVmPriorityMsg extends NeedReplyMessage implements HostMessage {
    private String vmInstanceUuid;

    private String hostUuid;

    private VmPriorityLevel level;

    public VmPriorityLevel getLevel() {
        return level;
    }

    public void setLevel(VmPriorityLevel level) {
        this.level = level;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}