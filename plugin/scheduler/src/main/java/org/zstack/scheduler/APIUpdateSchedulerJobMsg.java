package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.SchedulerJobVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/scheduler/jobs/{uuid}/actions",
        responseClass = APIUpdateSchedulerJobEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIUpdateSchedulerJobMsg extends APIMessage implements SchedulerMessage {
    @APIParam(resourceType = SchedulerJobVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false, emptyString = false)
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
    public String getSchedulerUuid() {
        return uuid;
    }


 
    public static APIUpdateSchedulerJobMsg __example__() {
        APIUpdateSchedulerJobMsg msg = new APIUpdateSchedulerJobMsg();
        msg.setUuid(uuid());
        msg.setName("Test2");
        msg.setDescription("new test");
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Updating").resource(uuid, SchedulerJobVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
