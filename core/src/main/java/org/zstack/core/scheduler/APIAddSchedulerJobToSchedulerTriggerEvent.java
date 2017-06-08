package org.zstack.core.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by AlanJager on 2017/6/8.
 */
@RestResponse
public class APIAddSchedulerJobToSchedulerTriggerEvent extends APIEvent{
    public APIAddSchedulerJobToSchedulerTriggerEvent() {
        super(null);
    }

    public APIAddSchedulerJobToSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }
}
