package org.zstack.header.network.l3;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 * Created by frank on 1/21/2016.
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
public class APICheckIpAvailabilityMsg extends APISyncCallMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true)
    private String l3NetworkUuid;
    @APIParam(maxLength = 255)
    private String ip;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
