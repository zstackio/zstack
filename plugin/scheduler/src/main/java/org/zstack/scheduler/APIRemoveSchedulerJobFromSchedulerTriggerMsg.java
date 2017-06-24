package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.SchedulerJobVO;
import org.zstack.header.core.scheduler.SchedulerTriggerVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by AlanJager on 2017/6/8.
 */

@RestRequest(
        path = "/scheduler/jobs/{schedulerJobUuid}/scheduler/triggers/{schedulerTriggerUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveSchedulerJobFromSchedulerTriggerEvent.class
)
public class APIRemoveSchedulerJobFromSchedulerTriggerMsg extends APIMessage {
    @APIParam(resourceType = SchedulerJobVO.class)
    private String schedulerJobUuid;

    @APIParam(resourceType = SchedulerTriggerVO.class)
    private String schedulerTriggerUuid;

    public APIRemoveSchedulerJobFromSchedulerTriggerMsg() {
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public String getSchedulerTriggerUuid() {
        return schedulerTriggerUuid;
    }

    public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public APIRemoveSchedulerJobFromSchedulerTriggerMsg(String schedulerJobUuid, String schedulerTriggerUuid) {
        super();
        this.schedulerJobUuid = schedulerJobUuid;
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Removed from trigger[uuid:%s]", schedulerTriggerUuid).resource(schedulerTriggerUuid, SchedulerTriggerVO.class.getSimpleName())
                            .context("schedulerTriggerUuid", schedulerTriggerUuid)
                            .messageAndEvent(that, evt).done();

                    ntfy("Removed from job[uuid:%s]", schedulerJobUuid).resource(schedulerJobUuid, SchedulerJobVO.class.getSimpleName())
                            .context("schedulerJobUuid", schedulerJobUuid)
                            .messageAndEvent(that,evt).done();
                }
            }
        };
    }
}
