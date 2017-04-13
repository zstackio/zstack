package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/15/2015.
 */
@Action(category = EipConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/eips/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateEipEvent.class
)
public class APIUpdateEipMsg extends APIMessage implements EipMessage {
    @APIParam(resourceType = EipVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    @Override
    public String getEipUuid() {
        return uuid;
    }
 
    public static APIUpdateEipMsg __example__() {
        APIUpdateEipMsg msg = new APIUpdateEipMsg();
        msg.setUuid(uuid());
        msg.setName("Test-EIP");

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Updated").resource(uuid, EipVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
