package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.utils.network.IPv6Constants;

public class SdnControllerUpdateDHCPMsg extends NeedReplyMessage implements SdnControllerMessage {
    private String l3NetworkUuid;
    private String sdnControllerUuid;
    private Integer ipVersion = IPv6Constants.DUAL_STACK;

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
}

