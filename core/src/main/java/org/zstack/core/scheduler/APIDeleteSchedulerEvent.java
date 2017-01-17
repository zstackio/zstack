package org.zstack.core.scheduler;

import org.zstack.header.message.APIEvent;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/15/16.
 */
public class APIDeleteSchedulerEvent extends APIEvent{

    public APIDeleteSchedulerEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteSchedulerEvent() {
        super(null);
    }
 
    public static APIDeleteSchedulerEvent __example__() {
        APIDeleteSchedulerEvent event = new APIDeleteSchedulerEvent();


        return event;
    }

}
