package org.zstack.header.vm;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class UpdateVmOSMsg extends NeedReplyMessage implements VmInstanceMessage{

    private String vmInstanceUuid;
    private String vmIp;
    @NoLogging
    private String vmPassword;
    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmIp() {
        return vmIp;
    }

    public void setVmIp(String vmIp) {
        this.vmIp = vmIp;
    }

    public String getPassword() {
        return vmPassword;
    }

    public void setPassword(String password) {
        this.vmPassword = password;
    }
}
