package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/router-interface-ip",
        method = HttpMethod.POST,
        responseClass = APISetL3NetworkRouterInterfaceIpEvent.class,
        parameterName = "params"
)
public class APISetL3NetworkRouterInterfaceIpMsg extends APIMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String l3NetworkUuid;

    @APIParam
    private String routerInterfaceIp;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getRouterInterfaceIp() {
        return routerInterfaceIp;
    }

    public void setRouterInterfaceIp(String routerInterfaceIp) {
        this.routerInterfaceIp = routerInterfaceIp;
    }

    public static APISetL3NetworkRouterInterfaceIpMsg __example__() {
        APISetL3NetworkRouterInterfaceIpMsg msg = new APISetL3NetworkRouterInterfaceIpMsg();
        msg.setL3NetworkUuid(uuid());
        msg.setRouterInterfaceIp("192.168.10.2");

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Set router interface ip[%s]", routerInterfaceIp).resource(l3NetworkUuid, L3NetworkVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
