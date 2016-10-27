package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmAccountPerference;

/**
 * Created by mingjian.deng on 16/10/26.
 */
public class SetRootPasswordMsg extends NeedReplyMessage implements HostMessage{
    private String hostUuid;
    private String qcowFile;
    private VmAccountPerference vmAccountPerference;

    public VmAccountPerference getVmAccountPerference() {
        return vmAccountPerference;
    }

    public void setVmAccountPerference(VmAccountPerference vmAccountPerference) {
        this.vmAccountPerference = vmAccountPerference;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getQcowFile() {
        return qcowFile;
    }

    public void setQcowFile(String qcowFile) {
        this.qcowFile = qcowFile;
    }
}
