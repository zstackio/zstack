package org.zstack.sdnController;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;

/**
 * Created by shixin.ruan on 06/26/2025.
 */
public class SdnControllerPingMsg extends NeedReplyMessage implements SdnControllerMessage {
    private String sdnControllerUuid;

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }
}
