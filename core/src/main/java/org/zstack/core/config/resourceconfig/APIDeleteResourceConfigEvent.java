package org.zstack.core.config.resourceconfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteResourceConfigEvent extends APIEvent {
    public APIDeleteResourceConfigEvent() {
    }

    public APIDeleteResourceConfigEvent(String apiId) {
        super(apiId);
    }
}
