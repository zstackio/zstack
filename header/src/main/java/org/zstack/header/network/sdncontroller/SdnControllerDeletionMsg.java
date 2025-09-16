package org.zstack.header.network.sdncontroller;

import org.zstack.header.message.DeletionMessage;

public class SdnControllerDeletionMsg extends DeletionMessage implements SdnControllerMessage {
    String sdnControllerUuid;

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }
}
