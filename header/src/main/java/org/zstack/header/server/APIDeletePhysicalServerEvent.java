package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeletePhysicalServerEvent extends APIEvent {

    public APIDeletePhysicalServerEvent(String apiId) {
        super(apiId);
    }

    public APIDeletePhysicalServerEvent() {
        super(null);
    }

    public static APIDeletePhysicalServerEvent __example__() {
        APIDeletePhysicalServerEvent event = new APIDeletePhysicalServerEvent();
        event.setSuccess(true);
        return event;
    }
}
