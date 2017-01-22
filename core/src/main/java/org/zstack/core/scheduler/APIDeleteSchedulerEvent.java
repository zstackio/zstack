package org.zstack.core.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/15/16.
 */
@RestResponse
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
