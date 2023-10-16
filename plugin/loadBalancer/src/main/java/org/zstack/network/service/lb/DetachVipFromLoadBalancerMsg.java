package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

public class DetachVipFromLoadBalancerMsg  extends NeedReplyMessage implements LoadBalancerMessage{
    private String uuid;
    private String vipUuid;

    private boolean hardDeleteDb;

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

    public boolean isHardDeleteDb() {
        return hardDeleteDb;
    }

    public void setHardDeleteDb(boolean hardDeleteDb) {
        this.hardDeleteDb = hardDeleteDb;
    }
}
