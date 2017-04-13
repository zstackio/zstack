package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/ip-ranges/by-cidr",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddIpRangeByNetworkCidrEvent.class
)
public class APIAddIpRangeByNetworkCidrMsg extends APICreateMessage implements L3NetworkMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String l3NetworkUuid;
    @APIParam
    private String networkCidr;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
 
    public static APIAddIpRangeByNetworkCidrMsg __example__() {
        APIAddIpRangeByNetworkCidrMsg msg = new APIAddIpRangeByNetworkCidrMsg();

        msg.setName("Test-IPRange");
        msg.setL3NetworkUuid(uuid());
        msg.setNetworkCidr("192.168.10.0/24");

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Added an IP range[%s]", networkCidr).resource(l3NetworkUuid, L3NetworkVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
