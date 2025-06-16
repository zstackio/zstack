package org.zstack.sdnController.header;

import org.zstack.header.message.NeedReplyMessage;

public class ReconnectSdnControllerMsg extends NeedReplyMessage implements SdnControllerMessage {
    String controllerUuid;

    public String getControllerUuid() {
        return controllerUuid;
    }

    public void setControllerUuid(String controllerUuid) {
        this.controllerUuid = controllerUuid;
    }

    @Override
    public String getSdnControllerUuid() {
        return controllerUuid;
    }
}
