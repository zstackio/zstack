package org.zstack.header.secret;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Request to define secret on KVM host (for VM e.g. vTPM). Caller provides plaintext DEK (dekBase64).
 * Host seals it with host public key (HPKE) and sends envelope to agent.
 */
public class SecretHostDefineMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String dekBase64;

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
}