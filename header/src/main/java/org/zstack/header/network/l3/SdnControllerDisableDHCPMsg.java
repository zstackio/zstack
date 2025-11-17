package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.utils.network.IPv6Constants;

public class SdnControllerDisableDHCPMsg extends NeedReplyMessage implements SdnControllerMessage {
    private String l3NetworkUuid;
    private Integer ipVersion = IPv6Constants.DUAL_STACK;
    private String sdnControllerUuid;
    private boolean checkIpRange = false;

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public boolean isCheckIpRange() {
        return checkIpRange;
    }

    public void setCheckIpRange(boolean checkIpRange) {
        this.checkIpRange = checkIpRange;
    }
}

