package org.zstack.network.service.vip;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VipDeletionMsg extends DeletionMessage implements VipMessage {
    private String vipUuid;

    @Override
    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }
}
