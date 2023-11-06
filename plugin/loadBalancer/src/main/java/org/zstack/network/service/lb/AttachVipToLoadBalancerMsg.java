package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

public class AttachVipToLoadBalancerMsg extends NeedReplyMessage implements LoadBalancerMessage{
    private String uuid;
    private String vipUuid;

    @Override
    public String getLoadBalancerUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }
}
