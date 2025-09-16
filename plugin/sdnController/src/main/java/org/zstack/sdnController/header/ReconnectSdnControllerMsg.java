package org.zstack.sdnController.header;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;

public class ReconnectSdnControllerMsg extends NeedReplyMessage implements SdnControllerMessage {
    private String controllerUuid;

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
