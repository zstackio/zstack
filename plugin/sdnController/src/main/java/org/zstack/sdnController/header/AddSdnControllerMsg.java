package org.zstack.sdnController.header;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.header.network.sdncontroller.SdnControllerVO;

public class AddSdnControllerMsg extends NeedReplyMessage implements SdnControllerMessage {
    SdnControllerVO sdnControllerVO;
    String accountUuid;

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerVO.getUuid();
    }

    public SdnControllerVO getSdnControllerVO() {
        return sdnControllerVO;
    }

    public void setSdnControllerVO(SdnControllerVO sdnControllerVO) {
        this.sdnControllerVO = sdnControllerVO;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
