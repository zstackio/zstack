package org.zstack.core.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.SchedulerJobSchedulerTriggerRefVO;
import org.zstack.header.core.scheduler.SchedulerTriggerVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by AlanJager on 2017/6/8.
 */

@RestRequest(
        path = "/scheduler/jobs/{schedulerJobUuid}/scheduler/triggers/{schedulerTriggerUuid}",
        method = HttpMethod.POST,
        responseClass = APIAddSchedulerJobToSchedulerTriggerEvent.class
)
public class APIAddSchedulerJobToSchedulerTriggerMsg extends APIMessage {
    @APIParam(resourceType = SchedulerJobSchedulerTriggerRefVO.class)
    private String schedulerJobUuid;

    @APIParam(resourceType = SchedulerTriggerVO.class)
    private String schedulerTriggerUuid;

    public APIAddSchedulerJobToSchedulerTriggerMsg() {
    }

    public APIAddSchedulerJobToSchedulerTriggerMsg(String schedulerJobUuid, String schedulerTriggerUuid) {
        super();
        this.schedulerJobUuid = schedulerJobUuid;
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }
}
