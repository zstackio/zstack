package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse
public class APIDeleteTagEvent extends APIEvent {
    public APIDeleteTagEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteTagEvent() {
        super(null);
    }
}
