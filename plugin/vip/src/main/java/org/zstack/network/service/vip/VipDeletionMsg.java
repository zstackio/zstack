package org.zstack.network.service.vip;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VipDeletionMsg extends DeletionMessage {
    private String vipUuid;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }
}
