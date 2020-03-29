package org.zstack.header.longjob;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIResumeLongJobEvent extends APIEvent {

    public APIResumeLongJobEvent() {
    }

    public APIResumeLongJobEvent(String apiId) {
        super(apiId);
    }
}
