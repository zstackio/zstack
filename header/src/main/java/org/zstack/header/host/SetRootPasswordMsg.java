package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 16/10/26.
 */
public class SetRootPasswordMsg extends NeedReplyMessage implements HostMessage{
    private String rootPassword;
    private String hostUuid;
    private String vmUuid;
    private String qcowFile;

    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getQcowFile() {
        return qcowFile;
    }

    public void setQcowFile(String qcowFile) {
        this.qcowFile = qcowFile;
    }
}
