package org.zstack.header.network.l3;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 * Created by frank on 6/15/2015.
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetFreeIpMsg extends APISyncCallMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class, required = false, checkAccount = true)
    private String l3NetworkUuid;
    @APIParam(resourceType = IpRangeVO.class, required = false, checkAccount = true)
    private String ipRangeUuid;

    private int limit = 100;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
