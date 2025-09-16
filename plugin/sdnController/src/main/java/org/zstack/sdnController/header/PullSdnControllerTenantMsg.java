package org.zstack.sdnController.header;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;

/**
 * Created by boce.wang on 06/13/2025.
 */
public class PullSdnControllerTenantMsg extends NeedReplyMessage implements SdnControllerMessage {
    private String sdnControllerUuid;

    public static PullSdnControllerTenantMsg fromApi(APIPullSdnControllerTenantMsg amsg) {
        PullSdnControllerTenantMsg msg = new PullSdnControllerTenantMsg();
        msg.setSdnControllerUuid(amsg.getSdnControllerUuid());
        return msg;
    }

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }
}
