package org.zstack.header.core.external.plugin;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRefreshPluginDriversEvent extends APIEvent {
    public APIRefreshPluginDriversEvent() { }

    public APIRefreshPluginDriversEvent(String apiId) {
        super(apiId);
    }

    public static APIRefreshPluginDriversEvent __example__() {
        return new APIRefreshPluginDriversEvent();
    }
}
