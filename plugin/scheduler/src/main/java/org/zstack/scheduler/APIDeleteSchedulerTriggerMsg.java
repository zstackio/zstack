package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.SchedulerTriggerVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by AlanJager on 2017/6/8.
 */

@Action(category = SchedulerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/scheduler/triggers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSchedulerTriggerEvent.class
)
public class APIDeleteSchedulerTriggerMsg extends APIDeleteMessage {
    @APIParam(resourceType = SchedulerTriggerVO.class, successIfResourceNotExisting = true)
    private String uuid;


    public APIDeleteSchedulerTriggerMsg() {

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }



    public static APIDeleteSchedulerTriggerMsg __example__() {
        APIDeleteSchedulerTriggerMsg msg = new APIDeleteSchedulerTriggerMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleting").resource(uuid, SchedulerTriggerVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
