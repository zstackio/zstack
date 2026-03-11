package org.zstack.header.secret;

import org.zstack.header.host.HostMessage;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Request to define secret on KVM host (for VM e.g. vTPM). Caller provides plaintext DEK (dekBase64).
 * Host seals it with host public key (HPKE) and sends envelope to agent.
 * vmUuid, purpose, providerName are required by key-agent for DEK cache key.
 */
public class SecretHostDefineMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    @NoLogging
    private String dekBase64;
    private String vmUuid;
    private String purpose;
    private String providerName;
    private String description;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getDekBase64() {
        return dekBase64;
    }

    public void setDekBase64(String dekBase64) {
        this.dekBase64 = dekBase64;
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

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
