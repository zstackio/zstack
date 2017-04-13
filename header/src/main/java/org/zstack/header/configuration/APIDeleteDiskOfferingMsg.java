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
        path = "/disk-offerings/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteDiskOfferingEvent.class
)
public class APIDeleteDiskOfferingMsg extends APIDeleteMessage implements DiskOfferingMessage {
    @APIParam(checkAccount = true, operationTarget = true)
    private String uuid;

    public APIDeleteDiskOfferingMsg() {
    }

    public APIDeleteDiskOfferingMsg(String uuid) {
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
    public String getDiskOfferingUuid() {
        return uuid;
    }
 
    public static APIDeleteDiskOfferingMsg __example__() {
        APIDeleteDiskOfferingMsg msg = new APIDeleteDiskOfferingMsg();
        msg.setUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleting").resource(uuid, DiskOfferingVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
