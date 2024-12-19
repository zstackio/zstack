package org.zstack.header.core.external.plugin;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeletePluginDriversEvent extends APIEvent {
    public APIDeletePluginDriversEvent() { }

    public APIDeletePluginDriversEvent(String apiId) {
        super(apiId);
    }

    public static APIDeletePluginDriversEvent __example__() {
        return new APIDeletePluginDriversEvent();
    }
}
