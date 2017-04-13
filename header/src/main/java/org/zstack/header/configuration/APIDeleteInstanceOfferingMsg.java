package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

@Action(category = ConfigurationConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/instance-offerings/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteInstanceOfferingEvent.class
)
public class APIDeleteInstanceOfferingMsg extends APIDeleteMessage implements InstanceOfferingMessage {
    @APIParam(resourceType = InstanceOfferingVO.class, successIfResourceNotExisting = true,
            checkAccount = true, operationTarget = true)
    private String uuid;

    public APIDeleteInstanceOfferingMsg() {
    }

    public APIDeleteInstanceOfferingMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getInstanceOfferingUuid() {
        return uuid;
    }
 
    public static APIDeleteInstanceOfferingMsg __example__() {
        APIDeleteInstanceOfferingMsg msg = new APIDeleteInstanceOfferingMsg();
        msg.setUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleting").resource(uuid, InstanceOfferingVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
