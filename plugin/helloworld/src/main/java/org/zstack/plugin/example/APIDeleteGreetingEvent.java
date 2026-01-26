package org.zstack.plugin.example;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteGreetingEvent extends APIEvent {
    public APIDeleteGreetingEvent() {
    }

    public APIDeleteGreetingEvent(String apiId) {
        super(apiId);
    }
}
