package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetVmDriverEvent extends APIEvent {
    public APISetVmDriverEvent() {
        super(null);
    }

    public APISetVmDriverEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmDriverEvent __example__() {
        APISetVmDriverEvent event = new APISetVmDriverEvent();
        return event;
    }
}
