package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = PortForwardingConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/port-forwarding/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIChangePortForwardingRuleStateEvent.class
)
public class APIChangePortForwardingRuleStateMsg extends APIMessage {
    @APIParam(resourceType = PortForwardingRuleVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }
 
    public static APIChangePortForwardingRuleStateMsg __example__() {
        APIChangePortForwardingRuleStateMsg msg = new APIChangePortForwardingRuleStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(PortForwardingRuleStateEvent.disable.toString());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Changed").resource(uuid,PortForwardingRuleVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
