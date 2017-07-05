package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:02 PM
 * To change this template use File | Settings | File Templates.
 */
@Action(category = ConfigurationConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/instance-offerings/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIChangeInstanceOfferingStateEvent.class
)
public class APIChangeInstanceOfferingStateMsg extends APIMessage implements InstanceOfferingMessage {
    @APIParam(resourceType = InstanceOfferingVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    @Override
    public String getInstanceOfferingUuid() {
        return getUuid();
    }

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
 
    public static APIChangeInstanceOfferingStateMsg __example__() {
        APIChangeInstanceOfferingStateMsg msg = new APIChangeInstanceOfferingStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(InstanceOfferingStateEvent.enable.toString());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Changing the state to %s", ((APIChangeInstanceOfferingStateEvent)evt).getInventory().getState())
                        .resource(uuid, InstanceOfferingVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
