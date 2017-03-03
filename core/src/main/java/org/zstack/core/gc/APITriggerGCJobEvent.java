package org.zstack.core.gc;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/3/5.
 */
@RestResponse
public class APITriggerGCJobEvent extends APIEvent {
    public APITriggerGCJobEvent() {
    }

    public APITriggerGCJobEvent(String apiId) {
        super(apiId);
    }
}
