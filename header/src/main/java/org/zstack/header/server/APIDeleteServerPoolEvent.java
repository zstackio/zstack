package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteServerPoolEvent extends APIEvent {
    public APIDeleteServerPoolEvent() {
        super(null);
    }

    public APIDeleteServerPoolEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteServerPoolEvent __example__() {
        return new APIDeleteServerPoolEvent();
    }
}
