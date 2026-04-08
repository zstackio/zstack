package org.zstack.header.secret;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Request to delete existing secret on KVM host by vmUuid/purpose/keyVersion.
 * keyVersion can be null, then agent may remove all matched versions for the vm+purpose.
 */
public class SecretHostDeleteMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmUuid;
    private String purpose;
    private Integer keyVersion;

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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }
}
