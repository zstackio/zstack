package org.zstack.core.scheduler;

import org.zstack.header.message.APIEvent;

/**
 * Created by root on 7/15/16.
 */
public class APIDeleteSchedulerEvent extends APIEvent{

    public APIDeleteSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteSchedulerEvent() {
        super(null);
    }
}
