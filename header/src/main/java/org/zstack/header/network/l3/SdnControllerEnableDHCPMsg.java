package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;

public class SdnControllerEnableDHCPMsg extends NeedReplyMessage implements SdnControllerMessage {
    private String l3NetworkUuid;
    private String sdnControllerUuid;

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
}
