package org.zstack.core.config;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRefreshGuestOsMetadataEvent extends APIEvent {
    public APIRefreshGuestOsMetadataEvent() {
    }

    public APIRefreshGuestOsMetadataEvent(String apiId) {
        super(apiId);
    }
}
