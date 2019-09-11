package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIEnableSpiceChannelSupportTLSEvent extends APIEvent {
    public APIEnableSpiceChannelSupportTLSEvent() {
    }

    public APIEnableSpiceChannelSupportTLSEvent(String apiId) {
        super(apiId);
    }

    public static APIEnableSpiceChannelSupportTLSEvent __example__() {
        APIEnableSpiceChannelSupportTLSEvent event = new APIEnableSpiceChannelSupportTLSEvent();
        return event;
    }
}
