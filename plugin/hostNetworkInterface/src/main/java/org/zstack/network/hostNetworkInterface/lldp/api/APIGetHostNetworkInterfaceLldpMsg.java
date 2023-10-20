package org.zstack.network.hostNetworkInterface.lldp.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.lldp.LldpConstant;

@Action(category = LldpConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/hostNetworkInterface/lldp/{interfaceUuid}/info",
        method = HttpMethod.GET,
        responseClass = APIGetHostNetworkInterfaceLldpReply.class
)
public class APIGetHostNetworkInterfaceLldpMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostNetworkInterfaceVO.class)
    private String interfaceUuid;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public static APIGetHostNetworkInterfaceLldpMsg __example__() {
        APIGetHostNetworkInterfaceLldpMsg msg = new APIGetHostNetworkInterfaceLldpMsg();
        msg.setInterfaceUuid(uuid());
        return msg;
    }
}

