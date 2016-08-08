package org.zstack.network.service.flat;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkMessage;

/**
 * Created by miao on 16-7-19.
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
public class APIGetL3NetworkDhcpIpAddressMsg extends APISyncCallMessage implements L3NetworkMessage {
    private String l3NetworkUuid;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
}
